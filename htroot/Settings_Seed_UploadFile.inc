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
        <input name="seedFilePath" type="text" id="fs_path" size="40" value="#[seedFilePath]#" />
      </div>
    </fieldset>
    <fieldset class="formControl">
      <div class="content">
        <button type="submit" name="seedFileSettings" class="primary icon-save">Save</button>
      </div>
    </fieldset>
  </form>
</fieldset>