import React, { useState } from "react";
import "./App.css";

function App() {
  const [url, setUrl] = useState("");
  const [quality, setQuality] = useState("137+140");
  const [logs, setLogs] = useState([]);
  const [downloading, setDownloading] = useState(false);
  const [downloadLink, setDownloadLink] = useState("");

  // ✅ Use environment variable with fallback
  const BACKEND_URL = process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";

  const handleDownload = () => {
    if (!url) return;

    setLogs([]);
    setDownloading(true);
    setDownloadLink("");

    const eventSource = new EventSource(
      `${BACKEND_URL}/api/youtube/download/stream?url=${encodeURIComponent(url)}&format=${quality}&t=${Date.now()}`
    );

    eventSource.onmessage = (event) => {
      const message = event.data;
      setLogs((prevLogs) => [...prevLogs, message]);

      if (message.startsWith("✅ Download completed")) {
        const fileName = message.split(":")[1]?.trim();
        if (fileName) {
          setDownloadLink(`${BACKEND_URL}/api/youtube/download-file/${fileName}`);
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
    { label: "Best (Auto)", value: "best" },
    { label: "8K", value: "315+140" },
    { label: "4K", value: "313+140" },
    { label: "1440p", value: "271+140" },
    { label: "1080p", value: "137+140" },
    { label: "720p", value: "136+140" },
    { label: "480p", value: "135+140" },
    { label: "360p", value: "134+140" },
    { label: "240p", value: "133+140" },
    { label: "144p", value: "160+140" },
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
