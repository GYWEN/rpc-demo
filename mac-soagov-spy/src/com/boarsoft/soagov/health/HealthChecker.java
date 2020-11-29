package com.boarsoft.soagov.health;

import java.util.List;
import java.util.Map;

public interface HealthChecker {

	void onFailed(String sn, Exception e);

	void init(Map<String, List<String>> prop);
}
