FROM centos:6

ENV TZ Asia/Shanghai
ENV LANG en_US.UTF-8

############################
# 1. 初始化CentOS6
############################
RUN mkdir /root/.ssh
RUN touch /root/.ssh/authorized_keys
RUN chmod 600 /root/.ssh/authorized_keys

RUN rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6
RUN sed -i "s/enabled=1/enabled=0/" /etc/yum/pluginconf.d/fastestmirror.conf
RUN echo "exclude=*.i386 *.i586 *.i686" >> /etc/yum.conf
COPY file/etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo

RUN yum install -y epel-release && rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-6
COPY file/etc/yum.repos.d/epel.repo /etc/yum.repos.d/epel.repo

RUN yum update -y

RUN yum -y install telnet openssl-devel iproute vim-enhanced wget curl screen sudo rsync tcpdump strace openssh-server openssh-clients cmake
RUN yum -y groupinstall "Development Tools"
RUN echo set fencs=utf-8,gbk >>/etc/vimrc

# 关闭SELINUX
RUN echo SELINUX=disabled>/etc/selinux/config
RUN echo SELINUXTYPE=targeted>>/etc/selinux/config

# 配置SSH服务
RUN echo "*               soft   nofile            65535" >> /etc/security/limits.conf
RUN echo "*               hard   nofile            65535" >> /etc/security/limits.conf
RUN sed -i "s/#UseDNS yes/UseDNS no/" /etc/ssh/sshd_config
RUN sed -i "s/latest yes/GSSAPIAuthentication no/" /etc/ssh/sshd_config
RUN sed -i "s/GSSAPICleanupCredentials yes/GSSAPICleanupCredentials no/" /etc/ssh/sshd_config
RUN sed -i "s/#MaxAuthTries 6/MaxAuthTries 10/" /etc/ssh/sshd_config
RUN sed -i "s/#ClientAliveInterval 0/ClientAliveInterval 30/" /etc/ssh/sshd_config
RUN sed -i "s/#ClientAliveCountMax 3/ClientAliveCountMax 10/" /etc/ssh/sshd_config

############################
# 2. 安装JAVA和Maven 
############################
RUN wget https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_linux-x64_bin.tar.gz
RUN tar -zxvf openjdk-11+28_linux-x64_bin.tar.gz
RUN mv jdk-11 /usr/local/
RUN ln -s /usr/local/jdk-11/bin/java /usr/bin/java
RUN echo "export JAVA_HOME=/usr/bin/java" >> ~/.bash_profile
RUN echo "export CLASSPATH=$JAVA_HOME/lib:$CLASSPATH" >> ~/.bash_profile
RUN echo "export PATH=$JAVA_HOME/bin:$PATH" >> ~/.bash_profile
RUN source ~/.bash_profile

RUN wget http://apache-mirror.rbc.ru/pub/apache/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz
RUN tar -zxvf apache-maven-3.8.6-bin.tar.gz
RUN mv apache-maven-3.8.6 /usr/local/
RUN ln -s /usr/local/apache-maven-3.8.6/bin/mvn /usr/bin/mvn
RUN echo "export M2_HOME=/usr/bin/mvn">>/etc/profile
RUN echo "export PATH=$MAVEN_HOME/bin:$PATH">>/etc/profile
COPY maven/settings.xml .m2/settimgs.xml
RUN source /etc/profile

############################
# 3. 安装Rust
############################
RUN curl -L https://static.rust-lang.org/rustup.sh | sh -s -- -y
RUN source /root/.cargo/env
COPY file/cargo/config .cargo/config

############################
# 4. 安装Nginx
############################
COPY pkg/rpm/nginx-1.18.0-1.el6.ngx.x86_64.rpm /tmp/
RUN yum install -y /tmp/nginx-1.18.0-1.el6.ngx.x86_64.rpm
COPY file/etc/nginx/nginx.conf /etc/nginx/nginx.conf

############################
# 5. 安装Redis6-解压安装
############################
WORKDIR /tmp/
COPY pkg/bin/redis-6.0.6_el6.x86_64.tar.gz /tmp/
RUN tar xf /tmp/redis-6.0.6_el6.x86_64.tar.gz -C /usr/local

# 创建用户
RUN useradd --home-dir /usr/local/redis-6.0.6/var/lib --no-create-home --user-group --shell /bin/bash --comment "Redis Database Server" redis
RUN chown -R root:root /usr/local/redis-6.0.6/
RUN chown -R redis:redis /usr/local/redis-6.0.6/var/

# 链接
RUN ln -s /usr/local/redis-6.0.6 /usr/local/redis
RUN ln -s /usr/local/redis/bin/* /usr/local/bin

# 复制配置文件
COPY file/usr/local/redis/etc/redis.conf /usr/local/redis/etc/redis.conf

############################
# 6. 安装PSQL解压安装
#######################
RUN yum -y install https://download.postgresql.org/pub/repos/yum/reporpms/EL-6-x86_64/pgdg-redhat-repo-latest.noarch.rpm
RUN yum install -y postgresql10 postgresql10-devel postgresql10-server
RUN service postgresql-10 initdb
RUN sed -i "s/local   all             all                                     peer/local   all             all                                     trust/" /var/lib/pgsql/10/data/pg_hba.conf
RUN sed -i "s/host    all             all             127.0.0.1\/32            ident/host    all             all             0.0.0.0\/0            trust/" /var/lib/pgsql/10/data/pg_hba.conf
RUN chown -R postgres /var/lib/pgsql/10/data/
RUN chmod -R 700 /var/lib/pgsql/10/data/

############################
# 7. 清理
############################
RUN rm -f /tmp/*.rpm /tmp/*.tar.gz
RUN yum clean all

############################
# 8.  创建项目文件夹
#     创建日志文件夹
############################
RUN mkdir -p /data/zhiyan/zhiyan-web-server/ /data/zhiyan/zhiyan-server /data/zhiyan/module/bin /data/zhiyan/module/conf/
RUN mkdir -p /var/log/zhiyan/zhiyan-server/ /var/log/zhiyan/zhiyan-web-server/

RUN useradd --comment ZhiYan --home-dir /var/lib/zhiyan --shell /sbin/nologin zhiyan
RUN chown -R zhiyan:zhiyan /var/lib/zhiyan /var/log/zhiyan

############################
# 9. 安装zhiyan-server
############################
COPY zhiyan-server/ zhiyan-server/
COPY zhiyan-server/conf/server.conf.sample /data/zhiyan/module/conf/server.conf
COPY zhiyan-server/conf/server.log.yaml.sample /data/zhiyan/module/conf/server.log.yaml
COPY /file/etc/rc.d/init.d/zhiyan-server /etc/rc.d/init.d/zhiyan-server
RUN chmod 755 /etc/rc.d/init.d//zhiyan-server

############################
# 10. 安装happy-java
############################
COPY happy-java/ happy-java/
RUN mvn -f happy-java/pom.xml compile
RUN mvn -f happy-java/ install

############################
# 11. 安装libzygrpc
############################
COPY libzygrpc/ libzygrpc/
COPY Cargo.toml Cargo.toml

############################
# 12. 安装zhiyan-web-server
############################
COPY zhiyan-web-server/ zhiyan-web-server/
RUN mvn -f zhiyan-web-server/ clean package
COPY zhiyan-web-server/src/main/resources/application.properties.sample /data/zhiyan/zhiyan-web-server/application.properties
COPY zhiyan-web-server/src/main/resources/logback.xml.sample /data/zhiyan/zhiyan-web-server/logback.xml
COPY /file/etc/rc.d/init.d/zhiyan-web-server /etc/rc.d/init.d/zhiyan-web-server
RUN chmod 755 /etc/rc.d/init.d/zhiyan-web-server

############################
# 13. 安装zhiyan-web
############################
COPY build/ /data/zhiyan/zhiyan-web

############################
# 14. 设置开机器启动
############################
COPY file/usr/local/bin/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod 755 /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

EXPOSE 80 22 5432 6379 9876 9090 9091

