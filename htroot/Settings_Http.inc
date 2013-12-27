<div id="devInfo" data-done="html5" data-skip="validators">
</div>

<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>HTTP Networking</legend>
    <fieldset class="formSection">
      <legend>Transparent Proxy</legend>
      <div class="content">
        <input type="checkbox" name="isTransparentProxy" id="trans_proxy" #(isTransparentProxy)#::checked="checked" #(/isTransparentProxy)#/>
        <label for="trans_proxy">Run YaCy as transparent proxy.</label>
        <p>
          <s class="sym sym-hint"></s>
          On linux you can configure your firewall to transparently redirect all http traffic through YaCy using this iptables rule:
          <br />
          <small><code>iptables -t nat -A PREROUTING -p tcp -s 192.168.0.0/16 --dport 80 -j DNAT --to 192.168.0.1:#[port]#</code></small>
        </p>
      </div>
    </fieldset>

    <fieldset class="formSection">
      <legend>Connection Keep-Alive</legend>
      <div class="content">
        <input type="checkbox" name="connectionKeepAliveSupport" id="conn_keep_alive" #(connectionKeepAliveSupport)#::checked="checked" #(/connectionKeepAliveSupport)#/>
        <label for="conn_keep_alive">YaCy should support the <abbr>HTTP</abbr> connection <code>keep-alive</code> feature.</label>
      </div>
    </fieldset>

    <fieldset class="formSection">
      <legend>Send <em>Via</em> Header</legend>
      <div class="content">
        <input type="checkbox" name="proxy.sendViaHeader" id="via_header" #(proxy.sendViaHeader)#::checked="checked" #(/proxy.sendViaHeader)#/>
        <label for="via_header">YaCy should send the <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.45">Via</a> <abbr>HTTP</abbr> header according to <em><abbr>RFC</abbr> 2616 Sect 14.45</em>.
        </label>
      </div>
    </fieldset>

    <fieldset class="formSection">
      <legend>Send "X-Forwarded-For" Header</legend>
      <div class="content">
        <input type="checkbox" name="proxy.sendXForwardedForHeader" id="x_forwarded_header" #(proxy.sendXForwardedForHeader)#::checked="checked" #(/proxy.sendXForwardedForHeader)#/>
        <label for="x_forwarded_header">YaCy should send the <code>X-Forwarded-For</code> <abbr>HTTP</abbr> header.</label>
      </div>
    </fieldset>

	<fieldset class="formSection">
      <legend>HTTP Server</legend>
      <div class="content">
		HTTP Server Version: <span class="tt">#[httpservername]#</span>
      </div>
    </fieldset>

    <fieldset class="formControl">
      <div class="description">
        <p>
          <s class="sym sym-hint"></s>
          Changes take effect immediately.
        </p>
      </div>
      <div class="content">
        <button type="submit" name="httpNetworking" class="primary icon-run">Save Configuration</button>
      </div>
    </fieldset>
  </fieldset>
</form>