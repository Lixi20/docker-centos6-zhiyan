package com.cdgeekcamp.zhiyan.webserver.Model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends CrudRepository<TokenDate, String> {

}
