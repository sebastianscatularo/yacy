<fieldset class="formGroup collapsible">
  <legend>FTP</legend>
  <div class="description">
    <p>
      This is the account for a <abr>FTP</abbr> server where you can host a seed-list file.
      If you set this, you will become a principal peer.
      Your peer will then upload the seed-bootstrap information periodically, but only if there had been changes to the seed-list.
    </p>
  </div>
  <form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
    <fieldset class="formSection">
      <legend>Server</legend>
      <div class="content">
        <input name="seedFTPServer" type="text" id="ftp_server" value="#[seedFTPServer]#" />
        <p>
          The host where you have a <abr>FTP</abbr> account, like <code>ftp.&lt;my-host&gt;.net</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Path</legend>
      <div class="content">
        <input name="seedFTPPath" type="text" id="ftp_path" value="#[seedFTPPath]#" />
        <p>
          The remote path on the <abr>FTP</abbr> server, like <code>yacy/seed.txt</code>. Missing sub-directories are <em>NOT</em> created automatically.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Username</legend>
      <div class="content">
        <input name="seedFTPAccount" type="text" id="ftp_user" value="#[seedFTPAccount]#" />
        <p>
          Your log-in at the <abr>FTP</abbr> server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Password</legend>
      <div class="content">
        <input name="seedFTPPassword" type="password" id="ftp_pwd" value="#[seedFTPPassword]#" />
        <p>
          Your log-in password at the <abr>FTP</abbr> server.
        </p>
      </div>
    </fieldset>
    <fieldset class="formControl">
      <div class="content">
        <button type="submit" name="seedFtpSettings" class="primary icon-save">Save</button>
      </div>
    </fieldset>
  </form>
</fieldset>