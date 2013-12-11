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
        <input name="seedScpServer" type="text" id="scpServer" value="#[seedScpServer]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter a hostname.
          </p>
        </div>
        <p>
          The host where you have an account, like <code>my.host.net</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Port</legend>
      <div class="content">
        <input name="seedScpServerPort" type="text" id="scpPort" value="#[seedScpServerPort]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter only digits (<code>0&mdash;9</code>).
          </p>
          <p class="error" data-id="range">
            Please enter a value between <code>0&mdash;65535</code>.
          </p>
        </div>
        <p>
          The <abbr>SSH</abbr> port of the host, like <code>22</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Path</legend>
      <div class="content">
        <input name="seedScpPath" type="text" id="scpPath" value="#[seedScpPath]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter a file location.
          </p>
        </div>
        <p>
          The remote path on the server, like <code>~/yacy/seed.txt</code>. Missing sub-directories are <em>NOT</em> created automatically.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Username</legend>
      <div class="content">
        <input name="seedScpAccount" type="text" id="scpUser" value="#[seedScpAccount]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter a username.
          </p>
        </div>
        <p>
          Your log-in at the server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Password</legend>
      <div class="content">
        <input name="seedScpPassword" type="password" id="scpPwd" value="#[seedScpPassword]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter the password for the specified user.
          </p>
        </div>
        <p>
          Your log-in password at the server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formControl">
      <div id="ycu-error-count-scp" class="description hiddden"></div>
      <div class="content">
        <button type="submit" id="submitSettingsScp" name="seedScpSettings" class="primary icon-save">Save</button>
      </div>
    </fieldset>
  </form>
</fieldset>