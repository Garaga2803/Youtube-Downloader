import React, { useState } from "react";

function App() {
  const [url, setUrl] = useState("");
  const [quality, setQuality] = useState("best");
  const [logs, setLogs] = useState([]);
  const [progress, setProgress] = useState(null);
  const [totalSizeMB, setTotalSizeMB] = useState(null);
  const [downloading, setDownloading] = useState(false);

  const startDownload = () => {
    setLogs([]);
    setProgress(null);
    setTotalSizeMB(null);
    setDownloading(true);

   const eventSource = new EventSource(
  `https://youtube-downloader-nhfx.onrender.com/api/youtube/download/stream?url=${encodeURIComponent(url)}&format=${quality}&t=${Date.now()}`
);


    eventSource.onmessage = (e) => {
      const message = e.data;

      if (message.startsWith("PROGRESS::")) {
        const [, percentStr, size, speed, eta] = message.split("::");
        const percent = parseFloat(percentStr);

        let remaining = null;
        if (totalSizeMB) {
          const completedFraction = percent / 100;
          const remainingMB = (1 - completedFraction) * totalSizeMB;
          remaining = remainingMB.toFixed(1) + " MiB";
        }

        setProgress({
          percent: isNaN(percent) ? null : percent.toFixed(1) + "%",
          size: size || "Calculating...",
          speed: speed || "Calculating...",
          eta: eta || "Calculating...",
          remaining
        });
      } else if (message.startsWith("SIZE::")) {
        const [, , size] = message.split("::");
        const sizeValue = parseFloat(size.replace("MiB", "").trim());
        setTotalSizeMB(sizeValue);
        setLogs((prev) => [...prev, `🧾 Estimated Size: ${size}`]);
      } else {
        setLogs((prev) => [...prev, message]);
      }
    };

    eventSource.onerror = () => {
      eventSource.close();
      setDownloading(false);
    };
  };

  return (
    <div style={{ padding: "20px", fontFamily: "Segoe UI", background: "#f8f9fa", minHeight: "100vh" }}>
      <h2 style={{ color: "#d63384" }}>🎬 YouTube Downloader</h2>

      <input
        style={{ width: "60%", padding: "10px", margin: "10px 0", borderRadius: "6px", border: "1px solid #ccc" }}
        type="text"
        placeholder="Enter YouTube URL"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
      />

      <select
        value={quality}
        onChange={(e) => setQuality(e.target.value)}
        style={{ padding: "10px", marginLeft: "10px", borderRadius: "6px", border: "1px solid #ccc" }}
      >
        <option value="best">Best</option>
        <option value="8k">8K</option>
        <option value="4k">4K</option>
        <option value="1080p">1080p</option>
        <option value="720p">720p</option>
        <option value="480p">480p</option>
        <option value="360p">360p</option>
      </select>

      <button
        onClick={startDownload}
        disabled={downloading}
        style={{
          padding: "10px 20px",
          marginLeft: "10px",
          background: downloading ? "#6c757d" : "#198754",
          color: "white",
          border: "none",
          borderRadius: "6px",
          cursor: "pointer",
        }}
      >
        {downloading ? "Downloading..." : "Start Download"}
      </button>

      {progress && (
        <div style={{ marginTop: "20px", padding: "15px", background: "#fff3cd", borderRadius: "8px" }}>
          <h4 style={{ margin: 0, color: "#856404" }}>📊 Download Progress</h4>
          <div>🔁 <strong style={{ color: "#0d6efd" }}>Progress:</strong> {progress.percent}</div>
          <div>📦 <strong style={{ color: "#198754" }}>Downloaded:</strong> {progress.size}</div>
          <div>🚀 <strong style={{ color: "#dc3545" }}>Speed:</strong> {progress.speed}/s</div>
          <div>⏳ <strong style={{ color: "#fd7e14" }}>ETA:</strong> {progress.eta}</div>
          {progress.remaining && (
            <div>📉 <strong style={{ color: "#20c997" }}>Remaining:</strong> {progress.remaining}</div>
          )}
        </div>
      )}

      <pre
        style={{
          background: "#e9ecef",
          padding: "15px",
          marginTop: "20px",
          borderRadius: "8px",
          maxHeight: "300px",
          overflowY: "auto",
          color: "#343a40"
        }}
      >
        {logs.join("\n")}
      </pre>
    </div>
  );
}

export default App;
