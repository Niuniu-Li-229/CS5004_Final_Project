import yfinance as yf

tsla = yf.Ticker("TSLA")

# 下载全部历史数据（2010年至今）
df = tsla.history(period="max")

# 保存为CSV
df.to_csv("TSLA_price.csv")
print(f"完成！共 {len(df)} 条记录")
