package com.github.im.group.gui.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface UserEndpoint {

	@GetExchange("/api/{owner}/{repo}")
	String getRepository(@PathVariable String owner, @PathVariable String repo);


}