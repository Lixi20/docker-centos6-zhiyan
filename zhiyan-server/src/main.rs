extern crate clap;
extern crate log;
extern crate log4rs;
extern crate zygrpc;

use std::{fs, thread};
use std::io::Write;
use std::sync::mpsc;
use clap::Arg;
use clap::Command;
use ini::Ini;
use local_ipaddress;
use log::*;
use signal_hook::consts::SIGINT;
use tokio::runtime::Builder;
use tonic::{Request, Response, Status};
use tonic::transport::Server;
use zygrpc::zhiyan_rpc::{ZhiYanRegisterModuleRequest, ZhiYanRegisterModuleResponse, ZhiYanRequest, ZhiYanResponse};
use zygrpc::zhiyan_rpc::zhi_yan_service_server::{ZhiYanService, ZhiYanServiceServer};
use crate::zhiyan::{path_exists, ZyServer};
use crate::zhiyan::server_conf::get_value_from_config_file;

mod zhiyan;

struct ZhiYanServer {
    zy_server: ZyServer,
}

macro_rules! get_table_name {
    ($name:expr,$version:expr) => {{
        if $name == "io"{
            let mut _tables_name: String = "ts_io".to_string();
            let version_vec: Vec<&str> = $version.split(".").collect();
            let big_version_code = version_vec[0].parse::<i8>().unwrap();
            let small_version_code = version_vec[1].parse::<i8>().unwrap();

            if big_version_code <= 4 && small_version_code < 18 {
                _tables_name = "ts_io_4_18_down".to_string();
            } else if big_version_code >= 5 && small_version_code > 5 {
                _tables_name = "ts_io_5_5_up".to_string();
            } else {
                _tables_name = "ts_io_4_18_up".to_string();
            }
            _tables_name
        }else {
            let _tables_name: String = format!("ts_{}",$name);
            _tables_name
        }
    }};
}

#[tonic::async_trait]
impl ZhiYanService for ZhiYanServer {
    async fn zymod(&self, request: Request<ZhiYanRequest>) -> Result<Response<ZhiYanResponse>, Status> {
        let mut postgresql_client = self.zy_server.connect_postgresql().await.unwrap();

        let mut _config_content = String::new();

        let mut _config_vec = Vec::new();

        let config_cache_file_name = format!("zhiyan_config_{}.cache", request.get_ref().host);

        if std::path::Path::new(&(config_cache_file_name)).exists() {
            _config_content = fs::read_to_string(config_cache_file_name).unwrap();
            _config_vec = _config_content.split("-:-").collect::<Vec<&str>>();
            let module_host = _config_vec[0].replace("\"", "");
            let module_name = _config_vec[1].replace("\"", "");
            let module_new_config = _config_vec[2];

            if request.get_ref().name == module_name && request.get_ref().host == module_host {
                return Ok(Response::new(ZhiYanResponse {
                    code: "10".to_string(),
                    message: module_new_config.to_string(),
                }));
            }
        }

        let table_name = get_table_name!(request.get_ref().name,request.get_ref().kernel_version);
        let columns_list = zhiyan::find_table_column_name(&mut postgresql_client, &*table_name).await.unwrap();

        zhiyan::insert_data(&mut postgresql_client, &*table_name, &*request.get_ref().host, &*request.get_ref().datetime, &*request.get_ref().content, columns_list).await.unwrap();

        let register_message = format!("{}模块数据已上传到数据库!", request.get_ref().name.replace("-", "_"));

        Ok(Response::new(ZhiYanResponse {
            code: "0".to_string(),
            message: register_message,
        }))
    }

    async fn zyregistermod(&self, request: Request<ZhiYanRegisterModuleRequest>) -> Result<Response<ZhiYanRegisterModuleResponse>, Status> {

        log::info!("接收到来自'{}'的'{}'注册请求",request.get_ref().host,request.get_ref().name);

        let mut redis_client = self.zy_server.connect_redis().unwrap();
        let mut postgresql_client = self.zy_server.connect_postgresql().await.unwrap();

        let config_cache_file_name = format!("zhiyan_config_{}.cache", request.get_ref().host);

        if std::path::Path::new(&(config_cache_file_name)).exists() {
            fs::remove_file(config_cache_file_name).expect("缓存清除失败");
        }

        let token_check = zhiyan::find_user_token_by_host(&mut postgresql_client, &*request.get_ref().host, &*request.get_ref().token).await.unwrap();

        //校验Token
        if token_check != "yes" {
            Ok(Response::new(ZhiYanRegisterModuleResponse {
                code: "1".to_string(),
                message: token_check,
            }))
        } else {
            //存模块配置文件
            let list_key = format!("ZhiYanConfig_{}_{}", request.get_ref().host,request.get_ref().kernel_version);
            redis_client.hset(&*list_key, &*request.get_ref().name, &*request.get_ref().config).expect("Redis数据插入失败");

            let tables_list = zhiyan::find_tables(&mut postgresql_client).await.unwrap();

            if tables_list[0] == "error" {
                Ok(Response::new(ZhiYanRegisterModuleResponse {
                    code: "1".to_string(),
                    message: tables_list[1].clone(),
                }))
            } else {
                let table_name = get_table_name!(request.get_ref().name,request.get_ref().kernel_version);

                if !tables_list.contains(&table_name) {
                    zhiyan::create_table(&mut postgresql_client, &*table_name, &*request.get_ref().content).await.unwrap();
                }

                let register_message = format!("Module:{}注册成功!", request.get_ref().name.replace("-", "_"));

                Ok(Response::new(ZhiYanRegisterModuleResponse {
                    code: "0".to_string(),
                    message: register_message,
                }))
            }
        }
    }
}

fn main() {
    zhiyan::interrupt_from_keyboard_handler(SIGINT).expect("[ERROR]信号捕获错误");

    let local_ipaddress = local_ipaddress::get().unwrap();

    let args = Command::new("zhiyan-server")
        .version("2.0.0")
        .about("智眼Server")
        .arg(
            Arg::new("config")
                .short('c')
                .long("conf")
                .help("指定智眼Server配置文件")
                .default_value("/etc/zhiyan/server.conf")
                .takes_value(true),
        )
        .arg(
            Arg::new("log")
                .short('l')
                .long("log_config")
                .help("指定智眼Server Log配置文件")
                .default_value("/etc/zhiyan/server.log.yaml")
                .takes_value(true),
        )
        .arg(
            Arg::new("ip")
                .short('i')
                .long("ipaddress")
                .value_name("IP")
                .help("指定智眼Server IP地址")
                .default_value(&local_ipaddress)
                .takes_value(true),
        )
        .get_matches();

    let (tx, rx) = mpsc::channel();

    let is_log_config_file_path_from_cmd_args = args.value_of("log").unwrap();
    let is_conf_file_path_from_cmd_args = args.value_of("config").unwrap();
    let is_server_ip_from_cmd_args = args.value_of("ip").clone().unwrap().to_string();

    if path_exists(is_log_config_file_path_from_cmd_args) {
        log4rs::init_file(
            is_log_config_file_path_from_cmd_args,
            Default::default(),
        ).expect("配置文件出错");
        log::info!("日志配置文件:'{}'加载成功。",is_log_config_file_path_from_cmd_args);
    } else {
        let default_log_config = zhiyan::build_log_config(LevelFilter::Info);
        log4rs::init_config(default_log_config).expect("默认配置文件出错");
        log::warn!("检测到日志配置文件:'{}'不存在,将加载默认日志配置(Level：Info)", is_log_config_file_path_from_cmd_args);
    }

    let config_file_path = args.value_of("config").unwrap().to_string().clone();

    tx.send(config_file_path).unwrap();

    let runtime = Builder::new_multi_thread().enable_all().build().unwrap();

    thread::spawn(move || {
        let received = rx.recv().unwrap();

        let conf = Ini::load_from_file(received).unwrap();

        let section = conf.section(Some("zyserver")).expect("配置文件缺少section");

        let redis_host = get_value_from_config_file(section, "redis_host");
        let redis_password = get_value_from_config_file(section, "redis_password");
        let redis_port = get_value_from_config_file(section, "redis_port");

        let fmt_redis_host = format!("redis://:{}@{}:{}/",redis_password,redis_host,redis_port);

        log::info!("redis监听地址：{}",fmt_redis_host.to_string().clone());

        let client = redis::Client::open(fmt_redis_host).expect("Redis连接失败");

        let mut con = client.get_connection().unwrap();
        let mut pubsub = con.as_pubsub();
        pubsub.subscribe("UpdateConfig").unwrap();

        loop {
            let msg = pubsub.get_message().unwrap();
            let payload: String = msg.get_payload().unwrap();
            let json: serde_json::Value = serde_json::from_str(&*payload).unwrap();

            let config_cache_file_name = format!("zhiyan_config_{}.cache", json["Host"].to_string().replace("\"", ""));
            let mut file = std::fs::File::create(config_cache_file_name).expect("缓存创建失败");
            let insert_cache = format!("{}-:-{}-:-{}", json["Host"], json["ModuleName"], payload);
            file.write_all(insert_cache.as_bytes()).expect("缓存写入失败");
        }
    });

    runtime.block_on(async {
        let zhiyan_service = ZhiYanServer {
            zy_server: ZyServer::new(is_conf_file_path_from_cmd_args)
        };

        let is_full_server_ip_address = format!("{}:{}", is_server_ip_from_cmd_args, zhiyan_service.zy_server.get_config_value().port);

        log::info!("ZhiYan Server启动地址：{}",is_full_server_ip_address);
        Server::builder()
            .add_service(ZhiYanServiceServer::new(zhiyan_service))
            .serve(is_full_server_ip_address.parse().unwrap())
            .await
            .unwrap();
    });
}
