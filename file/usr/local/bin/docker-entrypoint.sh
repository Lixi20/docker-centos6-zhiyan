#!/bin/sh

service sshd start

sleep 1

#sshd配置公钥
auth_lock_file=/var/log/docker_init_auth.lock

if [ ! -z "${PUBLIC_STR}" ]; then
    if [ -f ${auth_lock_file} ]; then
        echo "`date "+%Y-%m-%d %H:%M:%S"` [信息] 跳过添加公钥"
    else
        echo "${PUBLIC_STR}" >> /root/.ssh/authorized_keys

        if [ $? -eq 0 ]; then
            echo "`date "+%Y-%m-%d %H:%M:%S"` [信息] 公钥添加成功"
            echo `date "+%Y-%m-%d %H:%M:%S"` > ${auth_lock_file}
        else
            echo "`date "+%Y-%m-%d %H:%M:%S"` [错误] 公钥添加失败"
            exit 1
        fi
    fi
fi

# redis启动
rm -f /usr/local/redis/var/run/redis_6379.pid
/sbin/runuser -l redis -c "/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis.conf"

sleep 1

#Psql启动
service postgresql-10 start

sleep 1

psql -h 127.0.0.1 -p 5432 -U postgres -c "create user zhiyan with password 'aePhohw6lae0Kihah1ph';"

psql -h 127.0.0.1 -p 5432 -U postgres -c "create database zhiyan owner zhiyan;"

psql -h 127.0.0.1 -p 5432 -U zhiyan zhiyan -c "CREATE TABLE public.user_info (id bigserial NOT NULL,username varchar NOT NULL,"password" varchar NOT NULL);"

psql -h 127.0.0.1 -p 5432 -U zhiyan zhiyan -c "INSERT INTO public.user_info (username,"password") VALUES ('admin','zhiyan');"

psql -h 127.0.0.1 -p 5432 -U zhiyan zhiyan -c "CREATE TABLE public.user_module_stat_list (host varchar NOT NULL,module_name varchar NOT NULL,module_chart_type varchar NOT NULL,module_stat bool NOT NULL,module_chinese_name varchar NULL,module_unit varchar NULL,module_headers varchar NULL);"

psql -h 127.0.0.1 -p 5432 -U zhiyan zhiyan -c "CREATE TABLE public.user_token (id bigserial NOT NULL,datetime int8 NOT NULL,host varchar NOT NULL,"token" varchar NOT NULL);"

psql -h 127.0.0.1 -p 5432 -U zhiyan zhiyan -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO zhiyan;"
psql -h 127.0.0.1 -p 5432 -U zhiyan zhiyan -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO zhiyan;"

sleep 1

# 启动zhiyan-web-server
cp /tmp/zhiyan-web-server/target/zhiyan-web-server-1.0.0.jar /data/zhiyan/zhiyan-web-server/zhiyan-web-server-1.0.0.jar
ln -s /data/zhiyan/zhiyan-web-server/zhiyan-web-server-1.0.0.jar /data/zhiyan/zhiyan-web-server/lastest.jar
chkconfig --add zhiyan-web-server
service zhiyan-web-server start

sleep 1

# nginx启动
rm -f /var/run/nginx.pid
/usr/sbin/nginx

sleep 1

# 启动zhiyan-server
/root/.cargo/bin/cargo build --release
cp /tmp/target/release/zhiyan-server  /data/zhiyan/module/bin/
chkconfig --add zhiyan-server
service zhiyan-server start

# 保持前台运行，不退出
while true
do
    sleep 3600
done