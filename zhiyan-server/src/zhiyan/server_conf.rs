use ini::{Ini, Properties};

pub struct ZyServerConfig {
    pub active: bool,
    pub port: String,

    pub postgresql_username: String,
    pub postgresql_password: String,
    pub postgresql_host: String,
    pub postgresql_port: String,
    pub postgresql_database: String,

    pub redis_host: String,
    pub redis_port: String,
    pub redis_password: String,
    pub redis_database: String,
}

pub fn get_value_from_config_file(section: &Properties, key: &str) -> String {
    let message = format!("配置文件中缺少{}字段", key);
    return section.get(key).expect(&*message).to_string();
}

pub fn load_server_conf(filename: &str) -> ZyServerConfig {
    let conf = Ini::load_from_file(filename).unwrap();

    let section = conf.section(Some("zyserver")).expect("配置文件缺少section");

    let _conf_port = if get_value_from_config_file(section,"port") =="" {
        "9876".to_string()
    }else {
        get_value_from_config_file(section,"port")
    };


    let config_file_info = ZyServerConfig {
        active: section.get("active").unwrap().parse::<bool>().unwrap(),
        port: _conf_port.parse().unwrap(),
        postgresql_username: get_value_from_config_file(section, "postgresql_username"),
        postgresql_password: get_value_from_config_file(section, "postgresql_password"),
        postgresql_host: get_value_from_config_file(section, "postgresql_host"),
        postgresql_port: get_value_from_config_file(section, "postgresql_port"),
        postgresql_database: get_value_from_config_file(section, "postgresql_database"),
        redis_host: get_value_from_config_file(section, "redis_host"),
        redis_port: get_value_from_config_file(section, "redis_port"),
        redis_password: get_value_from_config_file(section, "redis_password"),
        redis_database: get_value_from_config_file(section, "redis_database")
    };
    return config_file_info;
}
