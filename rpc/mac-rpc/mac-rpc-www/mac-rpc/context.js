$(function() {
	var ctx = '/mac-rpc';
	App.macRpc = {
		'ctx' : ctx,
		'nodeIndex' : {
			url : ctx + '/node/index.htm'
		},
		'nodeMain' : {
			url : ctx + '/node/main.htm'
		},
		'refs' : {
			url : ctx + '/refs.htm'
		},
		'svcIndex' : {
			url : ctx + '/svc/index.htm'
		},
		'svcMain' :{
			url : ctx + '/svc/main.htm'
		},
		'methodIndex' :{
			url : ctx + '/method/index.htm'
		},
		'svcMonitor' :{
			url : ctx + '/svc/monitor.htm'
		},
		'svcBwControl' :{
			url : ctx + '/bw.htm'
		},
		'tpsLimit' :{
			url : ctx + '/tps.htm'
		}
	}
});