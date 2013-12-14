<fieldset class="main">
  <legend>System Status</legend>

  <fieldset class="formGroup collapsible">
    <legend>System</legend>
    <fieldset class="formSection">
      <legend>YaCy Version</legend>
      <div class="content">#[versionpp]#</div>
    </fieldset>
    #(peerStatistics)#::
    <fieldset class="formSection">
      <legend>Uptime</legend>
      <div class="content">#[uptime]#</div>
    </fieldset>
    #(/peerStatistics)#
    <fieldset class="formSection">
      <legend>Processors</legend>
      <div class="content">#[processors]#</div>
    </fieldset>
    <fieldset class="formSection">
      <legend>System Load</legend>
      <div class="content">#[load]#</div>
    </fieldset>
  </fieldset>

  <fieldset class="formGroup collapsible">
    <legend>Memory Usage</legend>
    <fieldset class="formSection">
      <legend>Free</legend>
      <div class="content">#[freeMemory]#</div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Total</legend>
      <div class="content">#[totalMemory]#</div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Max</legend>
      <div class="content">#[maxMemory]#</div>
    </fieldset>
    <fieldset class="formControl">
      <div class="content">
        <form class="noSpace" action="PerformanceMemory_p.html">
          <button class="icon-showMore" type="submit">View Memory Details</button>
        </form>
      </div>
    </fieldset>
  </fieldset>

  <fieldset class="formGroup collapsible">
    <legend>Protection</legend>
    <fieldset class="formSection">
      <legend>Protection</legend>
      <div class="content">
        #(protection)#
        <strong class="error-txt">Password is missing.</strong>
        ::
        <span class="ok-txt">password-protected</span>
        #(/protection)#
      </div>
    </fieldset>
    #(unrestrictedLocalAccess)#::
    <fieldset class="formSection">
      <legend>Access</legend>
      <div class="content">Unrestricted access from localhost.</div>
    </fieldset>
    #(/unrestrictedLocalAccess)#
    <fieldset class="formControl">
      <div class="content">
        <form class="noSpace" action="ConfigAccounts_p.html">
          <button class="icon-config" type="submit">Configure accounts</button>
        </form>
      </div>
    </fieldset>
  </fieldset>

  <fieldset class="formGroup collapsible">
    <legend>Network</legend>
    <fieldset class="formSection">
      <legend>Host</legend>
      <div class="content">
        #[host]#:#[port]#
        #(extPortFormat)#::
        &nbsp;, Binding to interface: #[extPort]#
        #(/extPortFormat)#
        #(sslSupport)#::
        &nbsp;, SSL: enabled
        #(/sslSupport)#
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Public Address</legend>
      <div class="content">
        #(peerAddress)#
        <span class="warning-txt">peer address not assigned</span>
        ::
        <a href="http://#[address]#">http://#[address]#</a>
        <br/>
        <a href="http://#[peername]#.yacy">http://#[peername]#.yacy</a> <em>(YaCy Address)</em>
        #(/peerAddress)#
      </div>
    </fieldset>
    #(portForwarding)#::
    <fieldset class="formSection">
      <legend>Port Forwarding Host</legend>
      <div class="content">
        #[host]#:#[port]# <em>(#(status)#broken::connected#(/status)#)</em>
      </div>
    </fieldset>
    #(/portForwarding)#
    <fieldset class="formSection">
      <legend>Traffic</legend>
      <div class="content">
        <p>
          Proxy: #[trafficProxy]#
          <br/>
          Crawler: #[trafficCrawler]#
        </p>
        <div class="p">
          <form class="noSpace" action="Status.html?ResetTraffic=true">
            <button class="icon-delete" type="submit">Reset traffic counters</button>
          </form>
        </div>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Incoming Connections</legend>
      <div class="content">
        <p>
          Active: #[connectionsActive]#
          <br/>
          Max: #[connectionsMax]#
        </p>
        <div class="p">
          <form class="noSpace" action="Connections_p.html">
            <button class="icon-showMore" type="submit">View details</button>
          </form>
        </div>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Port Forwarding Host</legend>
      <div class="content">
        #(seedServer)#
        <form class="noSpace" action="Settings_p.html?page=seed">
          <em>Disabled</em>&nbsp;&nbsp;<button class="icon-config" type="submit">Configure</button>
        </form>
        ::
        Enabled: Updating to server #[seedServer]#.
        <br/>
        Last upload: #[lastUpload]# ago.
        ::
        Enabled: Updating to file #[seedFile]#.
        <br/>
        Last upload: #[lastUpload]# ago.
        #(/seedServer)#
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Remote Proxy</legend>
      <div class="content">
        #(remoteProxy)#
        <em>not used</em>
        ::
        #[host]#:#[port]# <br/>
        Used for YaCy <i class="fa fa-arrow-right"></i> YaCy communication: #(4Yacy)#Yes::No #(/4Yacy)#
        #(/remoteProxy)#
      </div>
    </fieldset>
  </fieldset>

  <fieldset class="formGroup collapsible" id="queueMeters">
    <legend>Queues</legend>
    <fieldset class="formSection">
      <legend>Loader queue</legend>
      <div class="content">
        #[loaderQueueSize]#&nbsp;<progress max="#[loaderQueueMax]#" value="#[loaderQueueSize]#"></progress>&nbsp;#[loaderQueueMax]#
        <div class="p">
          <form class="noSpace" action="IndexCreateLoaderQueue_p.html">
            <button type="submit" class="icon-showMore">View loader queue</button>
          </form>
        </div>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Local crawl</legend>
      <div class="content">
        Size: #[localCrawlQueueSize]#
        #(localCrawlPaused)#::<em>(paused)</em>#(/localCrawlPaused)#
        <div class="p">
          <form class="noSpace inline" action="Status.html?#(localCrawlPaused)#pauseCrawlJob::continueCrawlJob#(/localCrawlPaused)#=&amp;jobType=localCrawl">
            <button type="submit" class="icon-run">#(localCrawlPaused)#pause local crawl::continue local crawl#(/localCrawlPaused)#</button>
          </form>
          <form class="noSpace inline" action="IndexCreateQueues_p.html?stack=LOCAL">
            <button type="submit" class="icon-run">View local queue</button>
          </form>
        </div>
      </div>
      <fieldset class="formSection">
        <legend>Remote crawl</legend>
        <div class="content">
          Size: #[remoteTriggeredCrawlQueueSize]#
          #(localCrawlPaused)#::<em>(paused)</em>#(/localCrawlPaused)#
          <div class="p">
            <form class="noSpace inline" action="Status.html?#(remoteTriggeredCrawlPaused)#pauseCrawlJob::continueCrawlJob#(/remoteTriggeredCrawlPaused)#=&amp;jobType=remoteTriggeredCrawl">
              <button type="submit" class="icon-run">#(remoteTriggeredCrawlPaused)#pause remote crawl::continue remote crawl#(/remoteTriggeredCrawlPaused)#</button>
            </form>
            <form class="noSpace inline" action="IndexCreateQueues_p.html?stack=REMOTE">
              <button type="submit" class="icon-run">View remote queue</button>
            </form>
          </div>
        </div>
      </fieldset>
      <fieldset class="formSection">
        <legend>Pre-Queueing</legend>
        <div class="content">
          #[stackCrawlQueueSize]#
        </div>
      </fieldset>
    </fieldset>
  </fieldset>

  <fieldset class="formSection">
    <legend>Auto-popup on start-up</legend>
    <div class="content">
      #(popup)#
      <form class="noSpace" action="Status.html?popup=true">
        <em>Disabled</em>&nbsp;&nbsp;<button type="submit" class="icon-config">Enable</button>
      </form>
      ::
      <form class="noSpace" action="Status.html?popup=false">
        <em>Enabled</em>&nbsp;&nbsp;<button type="submit" class="icon-config">Disable</button>
      </form>
      #(/popup)#
    </div>
  </fieldset>

  <fieldset class="formSection">
    <legend>Tray-Icon</legend>
    <div class="content">
      #(tray)#
      <form class="noSpace" action="ConfigProperties_p.html?tray=true">
        <em>Disabled</em>&nbsp;&nbsp;<button type="submit" class="icon-config">Enable</button>
      </form>
      ::
      <form class="noSpace" action="ConfigProperties_p.html?tray=false">
        <em>Enabled</em>&nbsp;&nbsp;<button type="submit" class="icon-config">Disable</button>
      </form>
      ::
      <form class="noSpace" action="Status.html?tray=true">
        <button type="submit" id="submitEnableTray" class="icon-config">Enable</button>
      </form>
      #(/tray)#
    </div>
  </fieldset>
</fieldset>