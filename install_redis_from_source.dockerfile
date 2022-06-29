####################
# 安装Redis6-编译安装
####################
# 安装GCC（网络安装太慢）
COPY pkg/rpm/centos-release-scl-7-4.el6.centos.noarch.rpm /tmp/
COPY pkg/rpm/devtoolset*.rpm /tmp/
RUN yum install -y /tmp/centos-release-scl-7-4.el6.centos.noarch.rpm && rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-SIG-SCLo
RUN yum install -y /tmp/devtoolset-9-gcc-9.1.1-2.5.el6.x86_64.rpm /tmp/devtoolset-9-make-4.2.1-1.el6.x86_64.rpm
ENV PATH="/opt/rh/devtoolset-9/root/usr/bin:/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin"

# 编译安装
COPY pkg/src/redis-6.0.6.tar.gz /tmp/
RUN tar xf /tmp/redis-6.0.6.tar.gz -C /tmp
WORKDIR /tmp/redis-6.0.6
RUN make -j 4 install PREFIX=/usr/local/redis-6.0.6
RUN mkdir -p /usr/local/redis-6.0.6/etc/ /usr/local/redis-6.0.6/var/{run,log,lib}

# 配置文件
RUN cp /tmp/redis-6.0.6/redis.conf /usr/local/redis-6.0.6/etc/
RUN sed -i 's#logfile ""#logfile "/usr/local/redis/var/log/redis.log"#' /usr/local/redis-6.0.6/etc/redis.conf
RUN sed -i 's#pidfile /var/run/redis_6379.pid#pidfile /usr/local/redis/var/run/redis_6379.pid#' /usr/local/redis-6.0.6/etc/redis.conf
RUN sed -i 's#dir ./#dir /usr/local/redis/var/lib/#' /usr/local/redis-6.0.6/etc/redis.conf
RUN sed -i 's#daemonize no#daemonize yes#' /usr/local/redis-6.0.6/etc/redis.conf

# 创建用户
RUN useradd --home-dir /usr/local/redis-6.0.6/var/lib --create-home --user-group --shell /bin/bash --comment "Redis Database Server" redis
RUN chown -R redis:redis /usr/local/redis-6.0.6/var/

# 链接
RUN ln -s /usr/local/redis-6.0.6 /usr/local/redis
RUN ln -s /usr/local/redis/bin/* /usr/local/bin

# 复制配置文件
COPY file/usr/local/redis/etc/redis.conf /usr/local/redis/etc/redis.conf