#include "mvg.h"
typedef ExpandableList String;
typedef struct curl_slist Header;
String* init_string()
{
	return init_list(sizeof(char));
}
static size_t write_func(void *ptr,
	size_t size,
	size_t nmemb,
	void* s)
{
	String* string = (String*)s;
	size_t realSize = size * nmemb;
	append_data(string,ptr,realSize);
	return realSize;
}
CURLcode get(const char* url,String* container,Header* header)
{
	assert(container != NULL && url != NULL);

	CURLcode res;
	static char null = '\0';
	CURL* curl = curl_easy_init();
	if(curl)
	{
			curl_easy_setopt(curl,CURLOPT_URL,url);
			curl_easy_setopt(curl,CURLOPT_WRITEFUNCTION,write_func);
			curl_easy_setopt(curl,CURLOPT_WRITEDATA,(void*)container);
			// set the buffer to maximum
			curl_easy_setopt(curl,CURLOPT_BUFFERSIZE,256000L);
			if(header)curl_easy_setopt(curl,CURLOPT_HTTPHEADER,header);
			res = curl_easy_perform(curl);
			curl_easy_cleanup(curl);
	}
	add_item(container,&null);
	return res;
}

Header* get_mvg_header()
{
	Header* header = NULL;
	// we accept json
	header = curl_slist_append(header,"Accept: application/json, text/javascript, */*; q=0.01");
	// get the authorization key...
	header = curl_slist_append(header,"X-MVG-Authorization-Key: 5af1beca494712ed38d313714d4caff6");
	// pretend that we come from their site
	header = curl_slist_append(header,"Referer: https://www.mvg.de/dienste/verbindungen.html");
	// pretend to be an ajax...
	header = curl_slist_append(header,"X-Requested-With: XMLHttpRequest");
	// pretend to be in a browser...
	header = curl_slist_append(header,"User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
	header = curl_slist_append(header,"Host: www.mvg.de");
	return header;
}

String* find_station_by_name(char* searchWord)
{
	Header* header = get_mvg_header();
	String* container = init_string();
	//CURLcode code;
	// construct the url
	int maxURLLength = 1000;
	char url[maxURLLength];
	// checking if the search word is too long.
	char* formatURL = "https://www.mvg.de/fahrinfo/api/location/queryWeb?q=%s";
	assert(strlen(formatURL) - 2 + strlen(searchWord) <= 1000);
	snprintf(url,maxURLLength,formatURL,searchWord);

	get(url,container,header);
	return container;
}
String* find_closest_station(float lat,float log)
{
	Header* header = get_mvg_header();
	String* container = init_string();
	//CURLcode code;
	// construct the url
	int maxURLLength = 1000;
	char url[maxURLLength];
	// checking if the search word is too long.

	char* formatURL = "https://www.mvg.de/fahrinfo/api/location/nearby?latitude=%f&longitude=%f";
	assert(strlen(formatURL) - 2 + strlen(searchWord) <= 1000);
	snprintf(url,maxURLLength,formatURL,searchWord);

	get(url,container,header);
	return container;
}
int main()
{
	String* stations = find_station_by_name("e");
	//printf("%s",stations -> items);
	clean_list(stations);
	return 0;
}
