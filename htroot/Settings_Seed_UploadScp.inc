<fieldset class="formGroup collapsible">
  <legend>SCP</legend>
  <div class="description">
    <p>
      This is the account for a server where you are able to login via <abbr>SSH</abbr>.
    </p>
  </div>

  <form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
    <fieldset class="formSection">
      <legend>Server</legend>
      <div class="content">
        <input name="seedScpServer" type="text" id="scp_serv" size="40" value="#[seedScpServer]#" />
        <p>
          The host where you have an account, like <code>my.host.net</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Port</legend>
      <div class="content">
        <input name="seedScpServerPort" type="text" id="scp_port" size="40" value="#[seedScpServerPort]#" />
        <p>
          The <abbr>SSH</abbr> port of the host, like <code>22</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Path</legend>
      <div class="content">
        <input name="seedScpPath" type="text" id="scp_path" size="40" value="#[seedScpPath]#" />
        <p>
          The remote path on the server, like <code>~/yacy/seed.txt</code>. Missing sub-directories are <em>NOT</em> created automatically.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Username</legend>
      <div class="content">
        <input name="seedScpAccount" type="text" id="scp_user" size="40" value="#[seedScpAccount]#" />
        <p>
          Your log-in at the server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Password</legend>
      <div class="content">
        <input name="seedScpPassword" type="password" id="scp_pwd" size="40" value="#[seedScpPassword]#" />
        <p>
          Your log-in password at the server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formControl">
      <div class="content">
        <button type="submit" name="seedScpSettings" class="primary icon-save">Save</button>
      </div>
    </fieldset>
  </form>
</fieldset>