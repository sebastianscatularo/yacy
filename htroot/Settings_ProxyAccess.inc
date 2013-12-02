<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>Proxy Access Settings</legend>
    <div class="description">
      <p>
        These settings configure the access method to your own <abbr>http</abbr> proxy and server.
        All traffic is routed throug one single port, for both proxy and server.<br />
      </p>
    </div>
    <fieldset class="formGroup">
      <legend>Administration Address &amp; Port</legend>
      <div class="description">
        <p>
          The socket addresses where YaCy should listen for incoming connections from other YaCy peers or http clients.<br />
          You have four possibilities to specify the address:
        </p>
        <ul>
          <li>defining a port only (<em>e.g. 8090</em>)</li>
          <li>defining IP address and port (<em>e.g. 192.168.0.1:8090</em>)</li>
          <li>defining host name and port (<em>e.g. home:8090</em>)</li>
          <li>defining interface name and port (<em>e.g. #eth0:8090</em>)</li>
        </ul>
        <p>
          <s class="sym sym-hint"></s>
          Dont forget to change your firewall configuration after you have changed the port.
          <br/>
          Changes will take effect in 5-10 seconds.
        </p>
      </div>
      <fieldset class="formSection">
        <legend>Address &amp; Port</legend>
        <div class="content">
          <input name="port" id="port" type="text" size="30" maxlength="30" value="#[port]#" />
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formGroup">
      <legend>Server Access Restrictions</legend>
      <div class="description">
        <p>
          <strong></strong><br />
          <br />
          You can restrict the access to this proxy/server using a two-stage security barrier:
        </p>
        <ul>
          <li>define an <em>access domain</em> with a list of granted client IP-numbers or with wildcards</li>
          <li>define an <em>user account</em> with an user:password - pair</li>
        </ul>
        <p>
          This is the account that restricts access to the proxy function.
          You probably don't want to share the proxy to the internet, so you should set the
          <abbr>IP</abbr>-Number Access Domain to a pattern that corresponds to you local intranet.
          The default setting should be right in most cases. If you want, you can also set a proxy account
          so that every proxy user must authenticate first, but this is rather unusual.
        </p>
      </div>
      <fieldset class="formSection">
        <legend>IP-Number filter</legend>
        <div class="content">
          <input name="proxyfilter" id="filter" type="text" size="50" maxlength="1000" value="#[proxyfilter]#" />
        </div>
      </fieldset>
      <fieldset class="formSection">
        <legend>Accounts</legend>
        <div class="content">
          <input name="use_proxyaccounts" id="accounts" type="checkbox" size="16" maxlength="16" value="on"#(use_proxyAccounts)#:: checked="checked"#(/use_proxyAccounts)# />
          Use <a href="ConfigAccounts_p.html">Proxy Accounts</a>
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formControl">
      <div class="content">
        <button type="submit" name="proxyaccount" class="primary icon-save">Save configuration</button>
      </div>
    </fieldset>

  </fieldset>
</form>
