package com.boarsoft.soagov.registry;

import java.util.List;

public interface ServiceRegistry {
	/**
	 * 返回暴露了此服务的所有节点（含所有版本）列表
	 * 
	 * @param sk
	 *            serviceKey 服务编号-服务（提供者）版本
	 * @return
	 */
	List<String> getProviders(String sk);
}
