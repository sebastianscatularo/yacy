<div id="devInfo" data-done="html5 validators">
</div>

<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data" id="settingsCrawler">
  <fieldset>
    <legend>Crawler Settings</legend>

    <fieldset class="formGroup">
      <legend>Generic</legend>
      <fieldset class="formSection">
        <legend>Timeout</legend>
        <div class="content">
          <input name="crawler.clientTimeout" id="crawlerClientTimeout" type="text" maxlength="16" value="#[crawler.clientTimeout]#" class="spinner"/>
          <div class="formHint hidden">
            <p class="error">
              Please enter only digits (<code>0&mdash;9</code>).
            </p>
            <p class="error" data-id="range">
              Please enter a value greater than <code>-1</code>.
            </p>
          </div>
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
          <input name="crawler.http.maxFileSize" id="crawlerHttpMaxFileSize" type="text" maxlength="16" value="#[crawler.http.maxFileSize]#" class="spinner"/>
          <div class="formHint hidden">
            <p class="error">
              Please enter only digits (<code>0&mdash;9</code>).
            </p>
            <p class="error" data-id="range">
              Please enter a value greater than <code>-2</code>.
            </p>
          </div>
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
          <input name="crawler.ftp.maxFileSize" id="crawlerFtpMaxFileSize" type="text" maxlength="16" value="#[crawler.ftp.maxFileSize]#" class="spinner"/>
          <div class="formHint hidden">
            <p class="error">
              Please enter only digits (<code>0&mdash;9</code>).
            </p>
            <p class="error" data-id="range">
              Please enter a value greater than <code>-2</code>.
            </p>
          </div>
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
          <input name="crawler.smb.maxFileSize" id="crawlerSmbMaxFileSize" type="text" size="16" maxlength="16" value="#[crawler.smb.maxFileSize]#" class="spinner"/>
          <div class="formHint hidden">
            <p class="error">
              Please enter only digits (<code>0&mdash;9</code>).
            </p>
            <p class="error" data-id="range">
              Please enter a value greater than <code>-2</code>.
            </p>
          </div>
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
          <input name="crawler.file.maxFileSize" id="crawlerFileMaxFileSize" type="text" size="16" maxlength="16" value="#[crawler.file.maxFileSize]#" class="spinner"/>
          <div class="formHint hidden">
            <p class="error">
              Please enter only digits (<code>0&mdash;9</code>).
            </p>
            <p class="error" data-id="range">
              Please enter a value greater than <code>-2</code>.
            </p>
          </div>
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
      <div id="ycu-error-count" class="description hiddden"></div>
      <div class="content">
        <button type="submit" id="submitSettings" name="crawlerSettings" class="primary icon-run">Save Configuration</button>
      </div>
    </fieldset>
  </fieldset>
</form>