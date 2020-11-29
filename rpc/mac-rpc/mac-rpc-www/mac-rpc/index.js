$(function(){
	App.code = 'mac-rpc';
	App.ctx = '/' + App.code;

	App.go2 = function(to) {
		switch (to) {
		case '':
		case '#':
		case '#mac-rpc':
		case '#mac-rpc/node':
			App.show({
				ctx: 'macRpc',
				action: 'nodeIndex'
			}, $.noop, App.main);
			break;
		case '#mac-rpc/svc':
			App.show({
				ctx: 'macRpc',
				action: 'svcIndex'
			}, $.noop, App.main);
			break;
		}
		//window.history.pushState('forward', null, './#forward');
	}
});
