package com.boarsoft.boar.mac.rpc.node.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boarsoft.bean.ReplyInfo;
import com.boarsoft.common.Util;
import com.boarsoft.common.dao.PagedResult;
import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcContext;

@Component
@Scope("prototype")
@RestController
@RequestMapping("/node")
public class RpcNodeAction {
	@Autowired
	protected RpcContext rpcContext;

	protected String addr;

	protected String key;

	@RequestMapping("/list")
	// @Authorized(code = "svc.inst.toggle")
	public ReplyInfo<Object> list(int pageNo, int pageSize) {
		Map<String, RpcRegistry> rrMap = rpcContext.getRegistryMap();
		int total = rrMap.size();
		List<String> addrLt = new ArrayList<String>(rrMap.size());
		addrLt.addAll(rrMap.keySet());
		Collections.sort(addrLt);
		int from = Math.max(0, pageNo - 1) * pageSize;
		int to = Math.min(addrLt.size(), pageNo * pageSize);
		List<String> aLt = addrLt.subList(from, to);
		List<Map<String, Object>> rmLt = new LinkedList<Map<String, Object>>();
		for (String key : aLt) {
			Map<String, Object> m = new HashMap<String, Object>();
			RpcRegistry rr = rrMap.get(key);
			m.put("addr", rr.getKey());
			m.put("version", Util.date2str(rr.getVersion(), Util.STDDTMF));
			rmLt.add(m);
		}
		PagedResult<Map<String, Object>> pr = //
				new PagedResult<Map<String, Object>>(total, rmLt, pageNo, pageSize);
		return new ReplyInfo<Object>(true, pr);
	}

	@RequestMapping("/get")
	// @Authorized(code = "svc.inst.toggle")
	public ReplyInfo<Object> get() {
		Map<String, RpcRegistry> rrMap = rpcContext.getRegistryMap();
		RpcRegistry rr = rrMap.get(addr);
		//
		Map<String, Object> infMap = new HashMap<String, Object>();
		infMap.put("addr", rr.getKey());
		infMap.put("tag", rr.getTag());
		infMap.put("version", Util.date2str(rr.getVersion(), Util.STDDTMF));
		infMap.put("meta", rr.getMeta());
		//
		Map<String, RpcReferenceConfig> rcMap = rr.getReferenceMap();
		List<Map<String, Object>> rcLt = new ArrayList<Map<String, Object>>();
		for (RpcReferenceConfig rc : rcMap.values()) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("id", rc.getId());
			m.put("group", rc.getGroup());
			m.put("name", rc.getName());
			m.put("interface", rc.getInterfaceName());
			m.put("version", rc.getVersion());
			m.put("type", rc.getType());
			m.put("timeout", rc.getTimeout());
			m.put("mocker", rc.getMocker());
			m.put("methods", rc.getMethodConfigMap().size());
			rcLt.add(m);
		}
		Map<String, RpcServiceConfig> scMap = rr.getServiceMap();
		List<Map<String, Object>> scLt = new ArrayList<Map<String, Object>>();
		for (RpcServiceConfig sc : scMap.values()) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("ref", sc.getRef());
			m.put("group", sc.getGroup());
			m.put("name", sc.getName());
			m.put("interface", sc.getInterfaceName());
			m.put("version", sc.getVersion());
			m.put("type", sc.getType());
			m.put("timeout", sc.getTimeout());
			m.put("mocker", sc.getMocker());
			m.put("id", sc.getId());
			m.put("methods", sc.getMethodConfigMap().size());
			scLt.add(m);
		}
		Map<String, Object> rm = new HashMap<String, Object>();
		rm.put("info", infMap);
		rm.put("refs", rcLt);
		rm.put("svcs", scLt);
		return new ReplyInfo<Object>(true, rm);
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
