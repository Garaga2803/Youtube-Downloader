import React, { useState, useEffect } from "react";
import "./App.css";

function App() {
  const [url, setUrl] = useState("");
  const [quality, setQuality] = useState("best");
  const [logs, setLogs] = useState([]);
  const [downloading, setDownloading] = useState(false);
  const [downloadLink, setDownloadLink] = useState("");

  const handleDownload = () => {
    if (!url) return;

    setLogs([]);
    setDownloading(true);
    setDownloadLink("");

    const eventSource = new EventSource(
      `https://youtube-downloader-nhfx.onrender.com/api/youtube/download/stream?url=${encodeURIComponent(
        url
      )}&format=${quality}&t=${Date.now()}`
    );

    eventSource.onmessage = (event) => {
      const message = event.data;

      setLogs((prevLogs) => [...prevLogs, message]);

      if (message.startsWith("✅ Download completed")) {
        const fileName = message.split(":")[1]?.trim();
        if (fileName) {
          setDownloadLink(
            `https://youtube-downloader-nhfx.onrender.com/api/youtube/download-file/${fileName}`
          );
        }
        eventSource.close();
        setDownloading(false);
      } else if (message.startsWith("❌")) {
        eventSource.close();
        setDownloading(false);
      }
    };

    eventSource.onerror = (err) => {
      console.error("SSE error:", err);
      setLogs((prevLogs) => [...prevLogs, "❌ Error connecting to server"]);
      setDownloading(false);
      eventSource.close();
    };
  };

  const qualityOptions = [
    { label: "Best", value: "best" },
    { label: "8K", value: "8k" },
    { label: "4K", value: "4k" },
    { label: "1440p", value: "1440p" },
    { label: "1080p", value: "1080p" },
    { label: "720p", value: "720p" },
    { label: "480p", value: "480p" },
    { label: "360p", value: "360p" },
    { label: "240p", value: "240p" }
  ];

  return (
    <div className="app">
      <h2>🎬 YouTube Downloader</h2>

      <input
        type="text"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
        placeholder="Enter YouTube URL"
        disabled={downloading}
      />

      <select
        value={quality}
        onChange={(e) => setQuality(e.target.value)}
        disabled={downloading}
      >
        {qualityOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      <button onClick={handleDownload} disabled={downloading || !url}>
        {downloading ? "Downloading..." : "Start Download"}
      </button>

      <div className="log-section">
        {logs.map((log, index) => (
          <div key={index} className="log-line">
            {log}
          </div>
        ))}
      </div>

      {downloadLink && (
        <div className="download-link">
          <a href={downloadLink} target="_blank" rel="noopener noreferrer">
            ⬇️ Click here to download the video
          </a>
        </div>
      )}
    </div>
  );
}

export default App;
