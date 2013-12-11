<div id="devInfo" data-done="html5 validators">
</div>

<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data" id="settingsServerAccess">
  <fieldset>
    <legend id="general">Server Access Settings</legend>
    <fieldset class="formGroup">
      <legend>IP Filter</legend>
      <div class="description">
        <p>
          <strong>Here you can restrict access to the server.</strong>
        </p>
        <p>
          By default, the access is not limited, because this function is needed to spawn the <abbr>P2P</abbr> index-sharing function.
          If you block access to your server (setting anything else than <code>*</code>), then you will also be blocked from using other peers' indexes for search service.
          However, blocking access may be correct in enterprise environments where you only want to index your company's own web pages.
        </p>
      </div>
      <fieldset class="formSection">
        <legend>IP-Number filter</legend>
        <div class="content">
          <input name="serverfilter" type="text" maxlength="1000" value="#[serverfilter]#" />
        </div>
      </fieldset>
    </fieldset>
    <fieldset class="formGroup">
      <legend>staticIP (optional)</legend>
      <div class="description">
        <p>
          <strong>The <em>staticIP</em> can help that your peer can be reached by other peers in case that your peer is behind a firewall or proxy.</strong>
        </p>
        <p>
          You can create a tunnel through the firewall/proxy (look out for <em>tunneling through https proxy with connect command</em>) and create an access point for incoming connections.
          This access address can be set here (either as <abbr>IP</abbr> number or domain name).
          If the address of outgoing connections is equal to the address of incoming connections, you don't need to set anything here, please leave it blank.
        </p>
        <p>
          <s class="sym sym-warning"></s>
          Your current <abbr>IP</abbr> is recognized as <code>#[clientIP]#</code>.
          If the value you enter here does not match with this <abbr>IP</abbr>,
          you will not be able to access the server pages anymore.
        </p>
      </div>

      <fieldset class="formSection">
        <legend>staticIP</legend>
        <div class="content">
          <input name="staticIP" id="staticIP" type="text" maxlength="80" value="#[staticIP]#" />
          <div class="formHint hidden">
            <p class="error">
              Please enter a valid IPv4 adress.
            </p>
          </div>
        </div>
      </fieldset>

      <fieldset class="formControl">
        <div id="ycu-error-count" class="description hiddden"></div>
        <div class="content">
          <button type="submit" id="submitSettings" name="serveraccount" class="primary icon-save">Save</button>
        </div>
      </fieldset>
    </fieldset>
  </fieldset>
</form>