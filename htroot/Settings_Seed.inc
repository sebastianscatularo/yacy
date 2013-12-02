<h2>Seed Upload Settings</h2>
<p>
  With these settings you can configure if you have an account on a public accessible
  server where you can host a seed-list file.
</p>

<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
  <fieldset>
    <legend>General</legend>
    <div class="description">
      <p>
        If you enable one of the available uploading methods, you will become a principal peer.
        Your peer will then upload the seed-bootstrap information periodically,
        but only if there have been changes to the seed-list.
      </p>
    </div>

    <fieldset class="formSection">
      <legend>Upload Method</legend>
      <div class="content">
        <select name="seedUploadMethod" id="method" size="1">
          #{seedUploadMethods}#
          <option value="#[name]#"#(selected)#:: selected="selected"#(/selected)#>#[name]#</option>
          #{/seedUploadMethods}#
        </select>
        <button type="submit" name="seedSettings" class="icon-save">Save</button>
        <p>
          Here you can specify which upload method should be used. Select 'none' to deactivate uploading.
        </p>
      </div>
    </fieldset>

    <fieldset class="formSection">
      <legend>Download URL</legend>
      <div class="content">
        <input name="seedURL" type="text" id="url" size="40" value="#[seedURL]#" />
        <p>
          The URL that can be used to retrieve the uploaded seed file, like <code>http://www.&lt;my-host&gt;.net/yacy/seed.txt</code>.
        </p>
      </div>
    </fieldset>

    <fieldset class="formControl">
      <div class="content">
        <button type="submit" name="seedUploadRetry" class="primary icon-run">Retry Uploading</button>
      </div>
    </fieldset>
  </fieldset>
</form>

<h2>Upload Methods Configuration</h2>
#{seedUploadMethods}#
#%[file]%#
#{/seedUploadMethods}#
</fieldset>