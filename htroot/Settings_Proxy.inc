<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>Remote Proxy (optional)</legend>
    <div class="description">
      <p>
        YaCy can use another proxy to connect to the internet. You can enter the address for the remote proxy here.
      </p>
    </div>

    <fieldset class="formGroup">
      <legend>Remote Proxy</legend>
      <fieldset class="formSection">
        <legend>Host</legend>
        <div class="content">
          <input name="remoteProxyHost" id="host" type="text" size="32" maxlength="128" value="#[remoteProxyHost]#" />
          <p>
            The <abbr>IP</abbr> address or domain name of the remote proxy.
          </p>
        </div>
      </fieldset>
      <fieldset class="formSection">
        <legend>Port</legend>
        <div class="content">
          <input name="remoteProxyPort" id="port" type="text" size="5" maxlength="5" value="#[remoteProxyPort]#" />
          <p>
            The port of the remote proxy.
          </p>
        </div>
      </fieldset>
      <fieldset class="formSection">
        <legend>Username</legend>
        <div class="content">
          <input name="remoteProxyUser" type="text" id="user" size="32" maxlength="128" value="#[remoteProxyUser]#" />
        </div>
      </fieldset>
      <fieldset class="formSection">
        <legend>Password</legend>
        <div class="content">
          <input name="remoteProxyPwd" id="pwd" type="password" size="32" maxlength="128" value="#[remoteProxyPwd]#" />
        </div>
      </fieldset>
      <fieldset class="formSection">
        <legend>No-proxy adresses</legend>
        <div class="content">
          <input name="remoteProxyNoProxy" type="text" id="no_prxy" size="32" maxlength="5000" value="#[remoteProxyNoProxy]#" />
          <p>
            <abbr>IP</abbr> addresses for which the remote proxy should not be used.
          </p>
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formGroup">
      <legend>Proxy Usage</legend>
      <fieldset class="formSection">
        <legend>Activate / Deactivate</legend>
        <div class="content">
          <input type="checkbox" name="remoteProxyUse" id="rem_prxy"#(remoteProxyUseChecked)#:: checked="checked"#(/remoteProxyUseChecked)# />
          <label for="rem_prxy">Use a remote proxy.</label>
        </div>
      </fieldset>

      <fieldset class="formSection">
        <legend>YaCy <i class="fa fa-arrow-right"></i> YaCy</legend>
        <div class="content">
          <input type="checkbox" name="remoteProxyUse4Yacy" id="prxy4yacy"#(remoteProxyUse4Yacy)#:: checked="checked"#(/remoteProxyUse4Yacy)# />
          <p>
            Specifies if the remote proxy should be used for the communication of this peer to other yacy peers.
          </p>
          <p>
            <s class="sym sym-hint"></s>
            Enabling this option could cause this peer to remain in junior status.
          </p>
        </div>
      </fieldset>

      <fieldset class="formSection">
        <legend>HTTPS</legend>
        <div class="content">
          <input type="checkbox" name="remoteProxyUse4SSL" id="prxy4ssl"#(remoteProxyUse4SSL)#:: checked="checked"#(/remoteProxyUse4SSL)# />
          <label for="prxy4ssl">Use remote proxy for HTTPS</label>
          <p>
            Specifies if YaCy should forward ssl connections to the remote proxy.
          </p>
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formControl">
      <div class="description">
        <p>
          <s class="sym sym-hint"></s>
          Changes take effect immediately.
        </p>
      </div>
      <div class="content">
        <button type="submit" name="proxysettings" class="primary icon-run"/>Save Configuration</button>
      </div>
    </fieldset>
  </fieldset>
</form>