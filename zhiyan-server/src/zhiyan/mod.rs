use std::error::Error;
use std::process::exit;
use std::{fs, thread};
use log::LevelFilter;
use log4rs::append::console::ConsoleAppender;
use log4rs::config::{Appender, Root};
use log4rs::encode::pattern::PatternEncoder;
use log4rs::Config;
use serde_json::Value;
use signal_hook::iterator::Signals;
use tokio_postgres::{Client, NoTls};
use regex::Regex;
use crate::zhiyan::server_conf::ZyServerConfig;

pub(crate) mod server_conf;

fn rem_first_and_last(value: &str) -> &str {
    let mut chars = value.chars();
    chars.next_back();
    chars.as_str()
}

//PostgreSQL
pub async fn find_tables(client: &mut Client) -> Result<Vec<String>, Box<(dyn std::error::Error + 'static)>> {

    let query = client.query("select tablename from pg_tables where schemaname = 'public';", &[]).await;

    match query {
        Ok(okk) => {
            let mut persons: Vec<String> = Vec::new();

            for row in okk {
                for i in 0..row.len() {
                    persons.push(row.get(i))
                }
            }
            Ok(persons)
        }
        Err(err) => {
            let response_message = format!("获取数据库列表失败,错误信息：{}",err);
            log::error!("{}",response_message);
            let mut persons: Vec<String> = Vec::new();
            persons.push("error".to_string());
            persons.push(response_message);
            Ok(persons)
        }
    }
}

pub async fn find_table_column_name(client: &mut Client, table_name: &str) -> Result<Vec<String>, Box<(dyn std::error::Error + 'static)>> {
    let mut persons: Vec<String> = Vec::new();

    let sql = format!("select column_name from information_schema.columns where table_name = '{}';", table_name);

    for row in client.query(sql.as_str(), &[]).await? {
        for i in 0..row.len() {
            persons.push(row.get(i))
        }
    }

    Ok(persons)
}

pub async fn insert_data(client: &mut Client, table_name: &str, host: &str, datetime: &str, module_content: &str, columns_list: Vec<String>) -> Result<(), Box<(dyn std::error::Error + 'static)>> {
    let mut columns_field = String::new();
    let mut value_field = String::new();

    if module_content.contains("rows") {
        let v: Value = serde_json::from_str(module_content)?;
        let a = v["rows"].as_array().unwrap();

        for i in 0..a.len() {
            columns_field = String::new();
            value_field = String::new();
            let colimns = a.get(i).unwrap().to_string();

            for s in 0..columns_list.len() {
                let key = columns_list.get(s).unwrap();
                if key != "id" && key != "host" && key != "datetime" {
                    columns_field.push_str(key);
                    columns_field.push_str(",");
                    let v: Value = serde_json::from_str(&*colimns)?;
                    let value = v[key].clone();
                    value_field.push_str(&*value.to_string().replace("\"", "'"));
                    value_field.push_str(",");
                }
            }
            let host_datetime = format!("'{}','{}'", host, datetime);
            let sql = format!("insert into {} ({}host,datetime) values ({}{});", table_name, columns_field, value_field, host_datetime);
            client.query(sql.as_str(), &[]).await?;
        }
        Ok(())
    } else {
        for i in 0..columns_list.len() {
            let key = columns_list.get(i).unwrap();
            if key != "id" && key != "host" && key != "datetime" {
                columns_field.push_str(key);
                columns_field.push_str(",");
                let v: Value = serde_json::from_str(module_content)?;
                let value = v[key].clone();
                value_field.push_str(&*value.to_string().replace("\"", "'"));
                value_field.push_str(",");
            }
        }
        let host_datetime = format!("'{}','{}'", host, datetime);
        let sql = format!("insert into {} ({}host,datetime) values ({}{});", table_name, columns_field, value_field, host_datetime);
        client.query(sql.as_str(), &[]).await?;
        Ok(())
    }
}

pub async fn create_table(client: &mut Client, table_name: &str, module_content: &str) -> Result<(), Box<(dyn std::error::Error + 'static)>> {
    let mut default_field = String::new();
    default_field.push_str("id bigserial NOT NULL,");
    default_field.push_str("datetime TIMESTAMPTZ NOT NULL,");
    default_field.push_str("host varchar NOT NULL,");


    let content = if module_content.contains("row") {
        let v: Value = serde_json::from_str(module_content)?;
        let a = v["rows"].clone();
        a[0].to_string()
    } else {
        module_content.to_string()
    };

    let content_replace = content.replace("{", "").replace("}", "").replace("\":", "-").replace("\"", "").replace(",", "-");

    let format_content: Vec<&str> = content_replace.split("-").collect();

    let mut key_list = Vec::new();

    for i in 0..format_content.len() / 2 {
        key_list.push(format_content.get(2 * i).expect("").to_string())
    }

    let v: Value = serde_json::from_str(&*content)?;

    let re = Regex::new(r"\d{4}-\d{1,2}-\d{1,2} \d{1,2}:\d{1,2}:\d{1,2}").unwrap();

    for i in 0..key_list.len() {
        let key = key_list[i].to_string();
        let value = v[key_list[i].to_string()].clone();

        if value.is_f64() {
            default_field.push_str(&*key);
            default_field.push_str(" float8 NOT NULL,")
        } else if value.is_i64() {
            default_field.push_str(&*key);
            default_field.push_str(" bigint NOT NULL,")
        } else if re.is_match(&*value.to_string()) {
            default_field.push_str(&*key);
            default_field.push_str(" TIMESTAMPTZ NOT NULL,")
        } else {
            default_field.push_str(&*key);
            default_field.push_str(" varchar NOT NULL,")
        }
    }

    let sql = format!("CREATE TABLE {} ({});", table_name, rem_first_and_last(&*default_field));
    client.query(sql.as_str(), &[]).await?;


    let hyper_table_sql = format!("SELECT create_hypertable('{}', 'datetime');",table_name);
    client.query(hyper_table_sql.as_str(), &[]).await?;

    Ok(())
}

pub async fn find_user_token_by_host(client: &mut Client, host: &str, token: &str) -> Result<String, Box<(dyn std::error::Error + 'static)>> {
    let mut persons: Vec<String> = Vec::new();

    let sql = format!("select token from user_token where host ='{}' LIMIT 1;", host);

    let query = client.query(sql.as_str(), &[]).await;

    match query {
        Ok(okk)=>{
            for row in okk {
                persons.push(row.get(0));
            }

            let token_from_db = persons[0].to_string();

            if token_from_db == token {
                Ok("yes".to_string())
            } else {
                let response_message = format!("Module注册失败，Token校验失败,Host:{}", host);
                Ok(response_message)
            }
        }
        Err(err) => {
            let response_message = format!("Token校验失败,错误信息：{}",err);
            log::error!("{}",response_message);
            Ok(response_message)
        }
    }
}

pub fn build_log_config(log_stdout_level: LevelFilter) -> Config {
    let stdout = ConsoleAppender::builder()
        .encoder(Box::new(PatternEncoder::new(
            "{d(%Y-%m-%d %H:%M:%S)} {P} [{l}] {m}{n}",
        )))
        .build();

    let config = Config::builder()
        .appender(Appender::builder().build("stdout", Box::new(stdout)))
        .build(
            Root::builder()
                .appender("stdout")
                .build(log_stdout_level.clone()),
        )
        .unwrap();

    return config;
}

pub fn interrupt_from_keyboard_handler(signum: i32) -> Result<(), Box<dyn Error>> {
    let mut signals = Signals::new(&[signum])?;

    thread::spawn(move || {
        for _sig in signals.forever() {
            println!("检测到用户发送终止信号，退出程序中......");
            exit(1)
        }
    });

    Ok(())
}

pub fn path_exists(path: &str) -> bool {
    fs::metadata(path).is_ok()
}

pub struct ZyServer {
    mod_conf: ZyServerConfig,
}

impl ZyServer {
    pub fn new(mod_conf_path: &str) -> Self {
        if !path_exists(&*mod_conf_path) {
            log::error!("智眼Server配置文件:{}不存在", mod_conf_path);
            exit(1)
        }

        return ZyServer {
            mod_conf: server_conf::load_server_conf(mod_conf_path),
        };
    }

    //PostgreSQL
    pub async fn connect_postgresql(&self) -> Result<Client, Box<(dyn std::error::Error + 'static)>> {
        let username = self.mod_conf.postgresql_username.clone();
        let password = self.mod_conf.postgresql_password.clone();
        let host = self.mod_conf.postgresql_host.clone();
        let port = self.mod_conf.postgresql_port.clone();
        let database = self.mod_conf.postgresql_database.clone();

        let conn_str = &format!(
            "postgres://{}{}{}@{}{}{}{}{}",
            username,
            if password.is_empty() { "" } else { ":" },
            password,
            host,
            if port.is_empty() { "" } else { ":" },
            port,
            if database.is_empty() { "" } else { "/" },
            database
        );

        let (client, connection) =
            tokio_postgres::connect(conn_str, NoTls).await?;

        tokio::spawn(async move {
            if let Err(e) = connection.await {
                eprintln!("数据库连接失败: {}", e);
            }
        });

        Ok(client)
    }

    //Redis
    pub fn connect_redis(&self) -> Result<simple_redis::client::Client, simple_redis::RedisError> {
        let connect_url = format!("redis://:{}@{}:{}/{}", self.mod_conf.redis_password, self.mod_conf.redis_host, self.mod_conf.redis_port, self.mod_conf.redis_database);

        let clients = simple_redis::create(&*connect_url);

        match clients {
            Ok(okk) => {
                Ok(okk)
            }
            Err(err) => {
                log::error!("Redis连接失败，错误信息：{}",err);
                exit(1)
            }
        }

    }

    pub fn get_config_value(&self) -> &ZyServerConfig {
        return &self.mod_conf;
    }
}
