import json
import csv
from datetime import datetime

with open("tsla_news.json", "r", encoding="utf-8") as f:
    news = json.load(f)

with open("tsla_news.csv", "w", newline="", encoding="utf-8") as f:
    writer = csv.writer(f)
    writer.writerow(["date", "datetime_unix", "headline", "summary", "source", "url", "category"])
    
    for item in news:
        # unix时间戳转日期
        date_str = datetime.fromtimestamp(item.get("datetime", 0)).strftime("%Y-%m-%d")
        writer.writerow([
            date_str,
            item.get("datetime", ""),
            item.get("headline", ""),
            item.get("summary", ""),
            item.get("source", ""),
            item.get("url", ""),
            item.get("category", "")
        ])

print("CSV转换完成！")
