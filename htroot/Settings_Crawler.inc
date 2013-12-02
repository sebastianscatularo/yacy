<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>Crawler Settings</legend>

    <fieldset class="formGroup">
      <legend>Generic</legend>
      <fieldset class="formSection">
        <legend>Timeout</legend>
        <div class="content">
          <input name="crawler.clientTimeout" type="text" size="16" maxlength="16" value="#[crawler.clientTimeout]#" class="spinner"/>
          <p>
            Connection timeout in ms. <code>0</code> means unlimited.
          </p>
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formGroup">
      <legend>HTTP Crawler Settings</legend>
      <fieldset class="formSection">
        <legend>Maximum Filesize</legend>
        <div class="content">
          <input name="crawler.http.maxFileSize" type="text" size="16" maxlength="16" value="#[crawler.http.maxFileSize]#" class="spinner"/>
          <p>
            Maximum allowed file size in bytes that should be downloaded. Larger files will be skipped. <code>-1</code> means unlimited.
            <br />
            <s class="sym sym-hint"></s>
            Please note that if the crawler uses content compression, this limit is used to check the compressed content size.
          </p>
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formGroup">
      <legend>FTP Crawler Settings</legend>
      <fieldset class="formSection">
        <legend>Maximum Filesize</legend>
        <div class="content">
          <input name="crawler.ftp.maxFileSize" type="text" size="16" maxlength="16" value="#[crawler.ftp.maxFileSize]#" class="spinner"/>
          <p>
            Maximum allowed file size in bytes that should be downloaded. Larger files will be skipped. <code>-1</code> means unlimited.
          </p>
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formGroup">
      <legend>SMB Crawler Settings</legend>
      <fieldset class="formSection">
        <legend>Maximum Filesize</legend>
        <div class="content">
          <input name="crawler.smb.maxFileSize" type="text" size="16" maxlength="16" value="#[crawler.smb.maxFileSize]#" class="spinner"/>
          <p>
            Maximum allowed file size in bytes that should be downloaded. Larger files will be skipped. <code>-1</code> means unlimited.
          </p>
        </div>
      </fieldset>
    </fieldset>

    <fieldset class="formGroup">
      <legend>Local File Crawler Settings</legend>
      <fieldset class="formSection">
        <legend>Maximum Filesize</legend>
        <div class="content">
          <input name="crawler.file.maxFileSize" type="text" size="16" maxlength="16" value="#[crawler.file.maxFileSize]#" />
          <p>
            Maximum allowed file size in bytes that should be downloaded. Larger files will be skipped. <code>-1</code> means unlimited.
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
        <button type="submit" name="crawlerSettings" class="primary icon-run"/>Save Configuration</button>
      </div>
    </fieldset>
  </fieldset>

</fieldset>
</form>