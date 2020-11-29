package com.boarsoft.soagov.config;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleSvcConfig implements SvcConfig, Serializable {
	private static final long serialVersionUID = 1L;

	/** */
	protected volatile int status = SvcConfig.STATUS_ENABLE;
	/** */
	protected String code;
	/** */
	protected volatile boolean bwConfigOn;
	/** key：维度值的组合 */
	protected Queue<BwConfig> bwCfgQu = new ConcurrentLinkedQueue<BwConfig>();
	/** */
	protected volatile boolean slaConfigOn;
	/** */
	protected Queue<SlaConfig> slaCfgQu = new ConcurrentLinkedQueue<SlaConfig>();

	public SimpleSvcConfig() {
	}

	public SimpleSvcConfig(String code) {
		this.code = code;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public Queue<BwConfig> getBwConfigs() {
		return bwCfgQu;
	}

	@Override
	public Queue<SlaConfig> getSlaConfigs() {
		return slaCfgQu;
	}

	@Override
	public void addSlaConfig(SlaConfig slaCfg) {
		this.delSlaConfig(slaCfg.getKey()); // 去除可能的重复
		this.slaCfgQu.add(slaCfg);
	}

	@Override
	public void delSlaConfig(String dks) {
		slaCfgQu.remove(new SlaConfigImpl(dks));
	}

	@Override
	public void clearSlaConfigs() {
		this.slaCfgQu.clear();
	}

	@Override
	public boolean isBwConfigOn() {
		return bwConfigOn;
	}

	@Override
	public void setBwConfigOn(boolean bwConfigOn) {
		this.bwConfigOn = bwConfigOn;
	}

	@Override
	public boolean isSlaConfigOn() {
		return slaConfigOn;
	}

	@Override
	public void setSlaConfigOn(boolean slaConfigOn) {
		this.slaConfigOn = slaConfigOn;
	}

	@Override
	public void addBwConfig(BwConfig bwCfg) {
		this.delBwConfig(bwCfg); // 去除可能的重复
		this.bwCfgQu.add(bwCfg);
	}

	@Override
	public void delBwConfig(BwConfig bwCfg) {
		this.bwCfgQu.remove(bwCfg);
	}
}
