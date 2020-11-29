package com.boarsoft.rpc.router;

/**
 * 
 * @author Mac_J
 *
 * @param <T>
 */
public interface RpcRouter<T> {
	/**
	 * 
	 * @param req
	 * @return
	 */
	String getProvider(T req);
}