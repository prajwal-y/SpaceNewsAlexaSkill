from bs4 import BeautifulSoup
import boto3
import datetime
import os
import requests
import sys
import time

SPACE_NEWS_SITEMAP = "http://www.space.com/sitemap.xml"
SPACE_NEWS_SITEMAP_TEMP_FILE = "/tmp/space_sitemap.xml"
SPACE_NEWS_DATA_TEMP_FILE = "/tmp/space_news_data.txt"
SPACE_NEWS_DATA_S3_BUCKET = "space-news-data"

DATA_ENTRY_DELIMITER = ":delim:"

ARTICLE_LIMIT = 20

def upload_file_to_s3():
	s3_client = boto3.client('s3')
	s3_client.upload_file(SPACE_NEWS_DATA_TEMP_FILE, 
		SPACE_NEWS_DATA_S3_BUCKET, str(datetime.date.today()))

def parse_sitemap_xml_and_fetch_news():
	file_handler = open(SPACE_NEWS_SITEMAP_TEMP_FILE).read()
	sitemap_xml = BeautifulSoup(file_handler, 'html.parser')
	f = open(SPACE_NEWS_DATA_TEMP_FILE, 'w')
	count = 0
	exception_count = 0
	for url in sitemap_xml.findAll('url'):
		try:
			article_response = requests.get(url.loc.string.strip(), 
				headers={'User-Agent': 'Mozilla/5.0'})
			article_data = BeautifulSoup(article_response.content, 'html.parser')

			article_header_tag = article_data.find("h1", { "class" : "h1" })
			article_header = article_header_tag.get_text().strip().replace('\n', ' ').replace('\r', '')

			article_content = article_data.find("div", 
				{ "class" : "article-content" })
			article_text = ""

			for p in article_content.findAll('p'):
				[s.extract() for s in p('script')]
				article_text += p.get_text().strip().replace('\n', ' ').replace('\r', '') + " "

			f.write(url.loc.string)
			f.write(DATA_ENTRY_DELIMITER)
			f.write(article_header.encode('utf-8'))
			f.write(DATA_ENTRY_DELIMITER)
			f.write(article_text.encode('utf-8'))
			f.write("\n")
		except:
			print "Unexpected error: " + url.loc.string, sys.exc_info()[0]
			exception_count += 1
			if exception_count > 20:
				break
			time.sleep(30)
			continue

		count += 1
		if count is ARTICLE_LIMIT:
			break
		time.sleep(30)
	f.close()
	upload_file_to_s3()
	os.remove(SPACE_NEWS_DATA_TEMP_FILE)

def start_data_collection():
	sitemap_xml = requests.get(SPACE_NEWS_SITEMAP, headers={'User-Agent': 'Mozilla/5.0'})
	f = open(SPACE_NEWS_SITEMAP_TEMP_FILE, 'w')
	f.write(sitemap_xml.content)
	f.close()
	parse_sitemap_xml_and_fetch_news()
	os.remove(SPACE_NEWS_SITEMAP_TEMP_FILE)

if __name__ == "__main__":
	start_data_collection()