import requests
import json
import time
from datetime import datetime, timedelta

API_KEY = "d7721chr01qtg3nfb7bgd7721chr01qtg3nfb7c0"
TICKER = "TSLA"
OUTPUT_FILE = "tsla_news.json"

# Tesla上市至今，按季度分批拉取（Finnhub单次最多返回1年内数据）
def generate_quarters(start_year, end_year):
    quarters = []
    for year in range(start_year, end_year + 1):
        quarters.append((f"{year}-01-01", f"{year}-03-31"))
        quarters.append((f"{year}-04-01", f"{year}-06-30"))
        quarters.append((f"{year}-07-01", f"{year}-09-30"))
        quarters.append((f"{year}-10-01", f"{year}-12-31"))
    return quarters

all_news = []
quarters = generate_quarters(2010, 2025)

for start, end in quarters:
    url = f"https://finnhub.io/api/v1/company-news?symbol={TICKER}&from={start}&to={end}&token={API_KEY}"
    response = requests.get(url)
    
    if response.status_code == 200:
        news = response.json()
        all_news.extend(news)
        print(f"{start} ~ {end}: 获取到 {len(news)} 条新闻")
    else:
        print(f"{start} ~ {end}: 请求失败 {response.status_code}")
    
    time.sleep(1)  # 避免触发频率限制

# 保存到本地
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump(all_news, f, ensure_ascii=False, indent=2)

print(f"\n完成！共 {len(all_news)} 条，已保存至 {OUTPUT_FILE}")
