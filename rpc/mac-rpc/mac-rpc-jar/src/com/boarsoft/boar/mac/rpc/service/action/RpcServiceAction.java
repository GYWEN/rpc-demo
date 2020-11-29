package com.boarsoft.boar.mac.rpc.service.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boarsoft.bean.ReplyInfo;
import com.boarsoft.boar.common.Constants;
import com.boarsoft.common.Util;
import com.boarsoft.common.dao.PagedResult;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcContext;
import com.boarsoft.rpc.core.RpcCore;
import com.boarsoft.rpc.core.RpcKeeper;
import com.boarsoft.rpc.core.RpcLink;
import com.boarsoft.soagov.config.BwConfig;
import com.boarsoft.soagov.config.BwConfigImpl;
import com.boarsoft.soagov.config.SlaConfig;
import com.boarsoft.soagov.config.SlaConfigImpl;
import com.boarsoft.soagov.spy.SvcSpy;

@Component
@Scope("prototype")
@RestController
@RequestMapping("/svc")
public class RpcServiceAction {
	private static final Logger log = LoggerFactory.getLogger(RpcServiceAction.class);
	/** 服务开关与升降级 */
	private static final int ACT_UP = 1;
	/** 服务开关与升降级 */
	private static final int ACT_DOWN = 2;
	/** 服务开关与升降级 */
	private static final int ACT_MOCK = 3;
	/** 增加或修改服务限流配置 */
	private static final int ACT_LIMIT_SAVE = 4;
	/** 移除服务限流配置 */
	private static final int ACT_LIMIT_REMOVE = 5;
	/** 启用服务限流、开启监控，是否真正限流取决于流量上限 */
	private static final int ACT_LIMIT_ON = 6;
	/** 停用服务限流、关闭监控 */
	private static final int ACT_LIMIT_OFF = 7;
	/** 增加或修改服务限流配置 */
	private static final int ACT_BW_SAVE = 8;
	/** 移除服务限流配置 */
	private static final int ACT_BW_REMOVE = 9;
	/** 启用黑白名单 */
	private static final int ACT_BW_ON = 10;
	/** 停用黑白名单 */
	private static final int ACT_BW_OFF = 11;

	@Autowired
	protected RpcContext rpcContext;
	@Autowired
	private RpcKeeper rpcKeeper;
	@Autowired
	private RpcCore rpcCore;

	@Autowired
	@Lazy(value = true)
	private SvcSpy svcSpy;

	protected String id;

	private String target;
	private String addr;
	private String key;
	private String type;
	private String mockType;
	private String mockJson;

	private short status;
	private int tpsLimit = -1;
	private int act;

	@RequestMapping("/get")
	// @Authorized(code = "rpc.svc.get")
	public ReplyInfo<Object> get() {
		List<Map<String, Object>> pLt = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> cLt = new ArrayList<Map<String, Object>>();
		Map<String, RpcRegistry> rrMap = rpcContext.getRegistryMap();
		for (RpcRegistry rr : rrMap.values()) {
			Map<String, RpcServiceConfig> scMap = rr.getServiceMap();
			if (scMap.containsKey(key)) {
				RpcServiceConfig sc = scMap.get(key);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("addr", rr.getKey());
				m.put("mocker", sc.getMocker());
				m.put("ref", sc.getRef());
				m.put("timeout", sc.getTimeout());
				m.put("type", sc.getType());
				m.put("id", sc.getId());
				m.put("methods", sc.getMethodConfigMap().size());
				m.put("status", 0);
				pLt.add(m);
			}
			Map<String, RpcReferenceConfig> rcMap = rr.getReferenceMap();
			if (rcMap.containsKey(key)) {
				RpcReferenceConfig rc = rcMap.get(key);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("addr", rr.getKey());
				m.put("mocker", rc.getMocker());
				m.put("id", rc.getId());
				m.put("timeout", rc.getTimeout());
				m.put("type", rc.getType());
				m.put("methods", rc.getMethodConfigMap().size());
				cLt.add(m);
			}
		}
		Map<String, Object> rm = new HashMap<String, Object>();
		rm.put("providers", pLt);
		rm.put("consumers", cLt);
		return new ReplyInfo<Object>(true, rm);
	}

	@RequestMapping("/list")
	// @Authorized(code = "rpc.svc.list")
	public ReplyInfo<Object> list(int pageNo, int pageSize) {
		Map<String, Integer> tpMap = new HashMap<String, Integer>();
		Map<String, Integer> trMap = new HashMap<String, Integer>();
		Map<String, RpcServiceConfig> tsMap = new HashMap<String, RpcServiceConfig>();
		//
		Map<String, RpcRegistry> rrMap = rpcContext.getRegistryMap();
		for (RpcRegistry rr : rrMap.values()) {
			Map<String, RpcServiceConfig> scMap = rr.getServiceMap();
			tsMap.putAll(scMap);
			for (String sk : scMap.keySet()) {
				Integer i = tpMap.get(sk);
				tpMap.put(sk, i == null ? 0 : i + 1);
			}
			Map<String, RpcReferenceConfig> rcMap = rr.getReferenceMap();
			for (String sk : rcMap.keySet()) {
				Integer i = trMap.get(sk);
				trMap.put(sk, i == null ? 0 : i + 1);
			}
		}
		List<Map<String, Object>> rmLt = new ArrayList<Map<String, Object>>();
		for (String sk : tsMap.keySet()) {
			RpcServiceConfig sc = tsMap.get(sk);
			//
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("group", sc.getGroup());
			m.put("name", sc.getName());
			m.put("interface", sc.getInterfaceName());
			m.put("version", sc.getVersion());
			m.put("sign", sc.getSign());
			m.put("providers", tpMap.get(sk));
			m.put("consumers", trMap.get(sk));
			//
			List<Map<String, Object>> dLt = new ArrayList<Map<String, Object>>();
			Map<String, RpcMethodConfig> mcMap = sc.getMethodConfigMap();
			for (String mk : mcMap.keySet()) {
				RpcMethodConfig mc = mcMap.get(mk);
				Map<String, Object> d = new HashMap<String, Object>();
				// d.put("callback", mc.getCallback());
				// d.put("failover", mc.getFailover());
				// d.put("mocker", mc.getMocker());
				// d.put("protocol", mc.getProtocol());
				// d.put("relativeId", mc.getRelativeId());
				d.put("sign", mc.getSign());
				// d.put("timeout", mc.getTimeout());
				// d.put("type", mc.getType());
				// d.put("uri", mc.getUri());
				dLt.add(d);
			}
			m.put("methods", dLt);
			//
			rmLt.add(m);
		}
		PagedResult<Map<String, Object>> pr = //
				new PagedResult<Map<String, Object>>(rmLt.size(), rmLt, pageNo, pageSize);
		return new ReplyInfo<Object>(true, pr);
	}

	@RequestMapping("/toggle")
	// @Authorized(code = "svc.inst.toggle")
	public ReplyInfo<Object> toggle() {
		if (act < 1 || act > 11) {
			return new ReplyInfo<Object>(Constants.ERROR_INVALID, "act");
		}
		if (this.toggle(addr, key, act)) {
			return ReplyInfo.SUCCESS;
		}
		return new ReplyInfo<Object>(Constants.ERROR_UNKNOWN);
	}

	private boolean toggle(String addr, String code, int act) {
		RpcContext.specify2(addr);
		try {
			switch (act) {
			case ACT_UP:
				return svcSpy.up(code);
			case ACT_MOCK:
				if (Util.strIsEmpty(mockType) || Util.strIsEmpty(mockJson)) {
					return svcSpy.mock(code);
				} else {
					try {
						return svcSpy.mock(code, mockType, mockJson);
					} catch (ClassNotFoundException e) {
						log.error("Mock type or json is invalid", e);
						return false;
					}
				}
			case ACT_DOWN:
				return svcSpy.down(code);
			case ACT_LIMIT_SAVE:
				if (Util.strIsEmpty(key)) {
					return svcSpy.limit(code, tpsLimit);
				} else {
					SlaConfig sc = new SlaConfigImpl(key);
					return svcSpy.limit(code, sc, tpsLimit);
				}
			case ACT_LIMIT_REMOVE:
				if (Util.strIsEmpty(key)) {
					return svcSpy.unlimit(code);
				} else {
					return svcSpy.unlimit(code, key);
				}
			case ACT_LIMIT_ON:
				return svcSpy.setSlaConfigOn(code, true);
			case ACT_LIMIT_OFF:
				return svcSpy.setSlaConfigOn(code, false);
			case ACT_BW_SAVE:
				BwConfig sc = new BwConfigImpl(key, status == 0);
				return svcSpy.addBwConfig(code, sc);
			case ACT_BW_REMOVE:
				return svcSpy.addBwConfig(code, new BwConfigImpl(key));
			case ACT_BW_ON:
				return svcSpy.setBwConfigOn(code, true);
			case ACT_BW_OFF:
				return svcSpy.setBwConfigOn(code, false);
			}
			return false;
		} catch (Exception e) {
			log.error("Error on toggle remote service {} at {}", code, addr, e);
			return false;
		} finally {
			RpcContext.specify2(null);
		}
	}

	/**
	 * 修改指定节点（target）的providerMap，将全部的provider或者指定的provider（addr）移除<br>
	 * 注：此方法只影响指定的节点，这些节点将无法再访问
	 */
	@RequestMapping("/enable")
	// @Authorized(code = "mac.rpc.svc.enable")
	public ReplyInfo<Object> enable() {
		try {
			this.toggle(RpcMethodConfig.ID_ENABLE_PROVIDER);
			return ReplyInfo.SUCCESS;
		} catch (Throwable e) {
			log.error("Error on invoke ENABLE_PROVIDER {}/{} on {} ", addr, key, target, e);
			return ReplyInfo.FAILED;
		}
	}

	@RequestMapping("/disable")
	// @Authorized(code = "mac.rpc.svc.disable")
	public ReplyInfo<Object> disable() {
		try {
			this.toggle(RpcMethodConfig.ID_DISABLE_PROVIDER);
			return ReplyInfo.SUCCESS;
		} catch (Throwable e) {
			log.error("Error on invoke DISABLE_PROVIDER {}/{} on {} ", addr, key, target, e);
			return ReplyInfo.FAILED;
		}
	}

	protected ReplyInfo<Object> toggle(int methodId) throws Throwable {
		// target为空表示要将某个服务上线或下线，广播通知所有节点，节点addr将开始提供或不再提供key服务
		if (Util.strIsEmpty(target)) {
			// addr为空表示所有节点都开始提供或不再提供key服务
			// key为表示指定节点开始提供或不再提供任何服务
			// addr和key两者都为空表示所有节点都开始提供或不再提供任何服务
			RpcMethodConfig mc = RpcContext.getMyMethodConfig(methodId);
			Map<String, Object> rm = rpcCore.broadcast(mc, new Object[] { addr, key }, null);
			return new ReplyInfo<Object>(true, rm);
		}
		String[] addrs = addr.split(",");
		for (String aimAddr : addrs) {
			// target不为空表示要通知某个节点，节点addr将不再（向其）提供key服务
			RpcLink lo = rpcCore.link2(target);
			// addr为空表示所有节点都开始提供或不再（向其）提供key服务
			// key为表示指定节点开始提供或不再（向其）提供任何服务
			// addr和key两者都为空表示所有节点都开始提供或不再（向其）提供任何服务
			lo.invoke(methodId, new Object[] { aimAddr, key });
		}
		return ReplyInfo.SUCCESS;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public RpcKeeper getRpcKeeper() {
		return rpcKeeper;
	}

	public void setRpcKeeper(RpcKeeper rpcKeeper) {
		this.rpcKeeper = rpcKeeper;
	}

	public RpcCore getRpcCore() {
		return rpcCore;
	}

	public void setRpcCore(RpcCore rpcCore) {
		this.rpcCore = rpcCore;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMockType() {
		return mockType;
	}

	public void setMockType(String mockType) {
		this.mockType = mockType;
	}

	public String getMockJson() {
		return mockJson;
	}

	public void setMockJson(String mockJson) {
		this.mockJson = mockJson;
	}

	public short getStatus() {
		return status;
	}

	public void setStatus(short status) {
		this.status = status;
	}

	public int getTpsLimit() {
		return tpsLimit;
	}

	public void setTpsLimit(int tpsLimit) {
		this.tpsLimit = tpsLimit;
	}

	public int getAct() {
		return act;
	}

	public void setAct(int act) {
		this.act = act;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
