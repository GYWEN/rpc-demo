package com.boarsoft.soagov.config;

import java.io.Serializable;
import java.util.Queue;

/**
 * 状态值与SvcInst的状态一一对应
 * 
 * @author Mac_J
 *
 */
public interface SvcConfig extends Serializable {
	/** */
	static final int STATUS_DOWN = 0;
	static final int STATUS_ENABLE = 1;
	static final int STATUS_DISABLE = 2;
	static final int STATUS_MOCKING = 3;

	/**
	 * 取得当前服务的开关状态
	 * 
	 * @return
	 */
	int getStatus();

	/**
	 * 设置当前服务的开关状态
	 * 
	 * @param status
	 */
	void setStatus(int status);

	void addSlaConfig(SlaConfig slaCfg);

	void clearSlaConfigs();

	boolean isBwConfigOn();

	void setBwConfigOn(boolean bwConfigOn);

	boolean isSlaConfigOn();

	void setSlaConfigOn(boolean slaConfigOn);

	void addBwConfig(BwConfig bwCfg);

	Queue<BwConfig> getBwConfigs();

	Queue<SlaConfig> getSlaConfigs();

	void delSlaConfig(String ck);

	void delBwConfig(BwConfig bwCfg);
}
