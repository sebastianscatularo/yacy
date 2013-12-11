<fieldset class="formGroup collapsible">
  <legend>Filesystem</legend>
  <div class="description">
    <p>
      You must configure this if you want to store the seed-list file onto the file system.
      <br/>
      Here you can specify the path within the filesystem where the seed-list file should be stored.
    </p>
  </div>
  <form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
    <fieldset class="formSection">
      <legend>File Location</legend>
      <div class="content">
        <input name="seedFilePath" type="text" id="fsPath" value="#[seedFilePath]#" />
        <div class="formHint hidden">
          <p class="error">
            Please enter a file location.
          </p>
        </div>
      </div>
    </fieldset>
    <fieldset class="formControl">
      <div id="ycu-error-count-fs" class="description hiddden"></div>
      <div class="content">
        <button type="submit" id="submitSettingsFs" name="seedFileSettings" class="primary icon-save">Save</button>
      </div>
    </fieldset>
  </form>
</fieldset>