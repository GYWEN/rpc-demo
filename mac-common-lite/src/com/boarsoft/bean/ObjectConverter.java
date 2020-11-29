package com.boarsoft.bean;

import java.io.IOException;

public interface ObjectConverter<A, B> {
	/**
	 * 将obj转换为某个固定类型的对象，具体是何类型由实现类自己决定<br>
	 * 通常，obj的类型是固定的
	 * 
	 * @param obj
	 * @return
	 * @throws ClassNotFoundException
	 */
	B convertAB(A a) throws ClassNotFoundException;

	/**
	 * 将obj转换为某个固定类型的对象，具体是何类型由实现类自己决定<br>
	 * 通常，obj的类型是固定的
	 * 
	 * @param obj
	 * @return
	 * @throws IOException 
	 */
	A convertBA(B b) throws IOException;
}
