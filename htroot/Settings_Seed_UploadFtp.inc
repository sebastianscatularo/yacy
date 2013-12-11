<fieldset class="formGroup collapsible">
  <legend>FTP</legend>
  <div class="description">
    <p>
      This is the account for a <abbr>FTP</abbr> server where you can host a seed-list file.
      If you set this, you will become a principal peer.
      Your peer will then upload the seed-bootstrap information periodically, but only if there had been changes to the seed-list.
    </p>
  </div>
  <form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
    <fieldset class="formSection">
      <legend>Server</legend>
      <div class="content">
        <input name="seedFTPServer" type="text" id="ftpServer" value="#[seedFTPServer]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter a hostname.
          </p>
        </div>
        <p>
          The host where you have a <abbr>FTP</abbr> account, like <code>ftp.&lt;my-host&gt;.net</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Path</legend>
      <div class="content">
        <input name="seedFTPPath" type="text" id="ftpPath" value="#[seedFTPPath]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter a file location.
          </p>
        </div>
        <p>
          The remote path on the <abbr>FTP</abbr> server, like <code>yacy/seed.txt</code>. Missing sub-directories are <em>NOT</em> created automatically.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Username</legend>
      <div class="content">
        <input name="seedFTPAccount" type="text" id="ftpUser" value="#[seedFTPAccount]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter a username.
          </p>
        </div>
        <p>
          Your log-in at the <abbr>FTP</abbr> server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Password</legend>
      <div class="content">
        <input name="seedFTPPassword" type="password" id="ftpPwd" value="#[seedFTPPassword]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter the password for the specified user.
          </p>
        </div>
        <p>
          Your log-in password at the <abbr>FTP</abbr> server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formControl">
      <div id="ycu-error-count-ftp" class="description hiddden"></div>
      <div class="content">
        <button type="submit" id="submitSettingsFtp" name="seedFtpSettings" class="primary icon-save">Save</button>
      </div>
    </fieldset>
  </form>
</fieldset>