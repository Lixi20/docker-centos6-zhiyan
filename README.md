# docker-centos6-zhiyan

基于CentOS6 + ZhiYan的Docker镜像

## 0. 构建镜像

### 克隆Dockerfile项目

#### 方法一（国内）

    git clone git@gitee.com:LiiLxx/docker-centos6-zhiyan.git

#### 方法二（全球）

    git clone git@github.com:Lixi20/docker-centos6-zhiyan.git

### 构建镜像

    cd docker-centos6-zhiyan
    sudo docker build -t docker-centos6-zhiyan:latest .

## 1. 环境组件列表

1. zhiyan-web-server
2. zhiyan-server
3. zhiyan-web
4. Nginx 1.8
5. Redis 6.0.6
6. PostgreSQL 10.21
7. Cargo 1.61.0
8. Rustup 1.24.3

## 2. 开发相关

### 2.1 开放端口

容器类的服务，默认监听 `0.0.0.0`：

* SSH->22
* Nginx->80
* PostgreSQL->5432
* Redis->6379
* zhiyan-web-server->9092
* zhiyan-server->9091
* zhiyan-web->9092

PostgreSQL、Redis的客户端工具可以连接容器内的服务端口，这样可以直接导入、导出、管理数据。

也能通过SSH+私钥方式连接容器的22端口，方便查看日志等等。

### 2.2 自定义设置

自定义配置参数，可以直接通过Docker命令进入bash编辑：

    docker exec -it 容器名称 bash

或者通过SSH+私钥方式连接容器的22端口：

    ssh 容器IP

## 3. 使用方法

### 3.1 启动一个容器很简单

    docker run docker-centos6-zhiyan:latest

### 3.2 启动容器时暴露端口

    docker run -p 8080:80 --name docker-centos6-zhiyan:latest

## 4. 环境配置

### 4.1 配置文件

[NOTE]
Reids、PostgreSQL密码默认为'aePhohw6lae0Kihah1ph',如需修改请按照以下教程。

#### 4.1.1 zhiyan-web-server

zhiyan-web-server主配置文件：

    /data/zhiyan/zhiyan-web-server/application.properties

#### 4.1.2 Nginx

Nginx主配置文件:

    /etc/nginx/nginx.conf

Nginx Host配置文件:

    /etc/nginx/conf.d

Web目录:

    /data/zhiyan/zhiyan-web

#### 4.1.3 zhiyan-server

zhiyan-server主配置文件：

    /data/zhiyan/module/conf/server.conf

#### 4.1.4 Redis

Redis主配置文件:

    /usr/local/redis/etc/redis.conf

### 4.2 日志文件

#### 4.2.1 zhiyan-web-server

zhiyan-web-server主日志文件：

    /data/zhiyan/zhiyan-web-server/logback.xml

#### 4.2.2 zhiyan-server

zhiyan-server主日志文件：

    /data/zhiyan/module/conf/server.log.yaml

### 4.3 默认端口

zhiyan-web-server默认端口：

    9092

zhiyan-serevr默认端口：

    9091

### 4.4 运行目录

#### 4.4.1 zhiyan-web-server

日志目录：

    /var/log/zhiyan/zhiyan-web-server

#### 4.4.2 Nginx

日志目录:

    /var/log/nginx

#### 4.4.3 zhiyan-server

日志目录：

    /var/log/zhiyan/zhiyan-server/

#### 4.4.4 Redis

日志目录:

    /usr/local/redis/var/log

### 5. 修改默认密码

#### 5.1 Redis

Redis本地文件：

    file/usr/local/redis/etc/redis.conf

    sed -i "s/requirepass aePhohw6lae0Kihah1ph/requirepass ********/" file/usr/local/redis/etc/redis.conf

zhiyan-web-server本地文件:

    zhiyan-web-server/src/main/resources/application.properties

    sed -i "s/spring.redis.password=aePhohw6lae0Kihah1ph/spring.redis.password=********/" zhiyan-web-server/src/main/resources/application.properties

zhiyan-server本地文件：

    zhiyan-server/conf/server.conf

    sed -i "s/redis_password=aePhohw6lae0Kihah1ph/redis_password=********/" zhiyan-server/conf/server.conf

#### 5.2 PostgreSQL

docker入口文件：

    file/usr/local/bin/docker-entrypoint.sh  

    sed -i "s/psql -h 127.0.0.1 -p 5432 -U postgres -c \"create user zhiyan with password 'aePhohw6lae0Kihah1ph';\"/psql -h 127.0.0.1 -p 5432 -U postgres -c \"create user zhiyan with password '********';\"/" file/usr/local/bin/docker-entrypoint.sh

zhiyan-web-server本地文件：

    zhiyan-web-server/src/main/resources/application.properties

    sed -i "s/spring.datasource.password=aePhohw6lae0Kihah1ph/spring.datasource.password=********/" zhiyan-web-server/src/main/resources/application.properties

zhiyan-server本地文件：

    zhiyan-server/conf/server.conf

    sed -i "s/postgresql_password=aePhohw6lae0Kihah1ph/postgresql_password=********/" zhiyan-server/conf/server.conf

### 6. Cargo换源

[NOTE]
如果在docker容器里面使用 cargo build 或者 cargo build --release 出现慢、失败的情况，请将Cargo换源。

本地源文件：

   file/cargo/config

容器源文件：

    ~/.cargo/config


### 7. 服务状态

#### 7.1 zhiyan-web-server

开机自启

    chkconfig zhiyan-server on

启动

    service zhiyan-web-server start

查看状态

    service zhiyan-web-server status

停止

    service zhiyan-web-server stop

#### 7.2 zhiyan-serevr

开机自启

    chkconfig zhiyan-web-server on

启动

    service zhiyan-server start 

查看状态

    service zhiyan-server status 

停止

    service zhiyan-server stop

