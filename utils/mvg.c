#include "mvg.h"
typedef ExpandableList String;
typedef struct curl_slist Header;

/*
	JSON functions
*/
//#define ASSERT_JSON_TYPE(j,t) assert(json_value_get_type(j)==t)

JSON_Value* string_to_json(String* str)
{
	return json_parse_string((const char*)(str -> items));
}
/*
	mvg functions
*/
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

	char* formatURL = "https://www.mvg.de/fahrinfo/api/location/nearby?latitude=%5f&longitude=%5f";
	snprintf(url,maxURLLength,formatURL,lat,log);

	get(url,container,header);
	return container;
}

void update_station_list()
{
	// TODO: fetch station names from list and put it to database
}


// aux. function for checking JSON type
void assert_type(const JSON_Value *value,enum json_value_type expectedType)
{
	if(value == NULL)
	{
		printf("value is null!\n");
		exit(1);
	}
	if(json_value_get_type(value) != expectedType)
	{
		printf("failed to expect %d",expectedType);
		exit(1);
	}
}
// aux. function to help cleaning up array of arrays...
void __cleanList(void* item)
{
	free(item);
}
char** get_stations(ParseServerOpt* opt,int* size)
{
	static const char* emptyQuery = "",
				* expectResponseSchema = "{\"stations\":[]}";
	int i,j, numStations,numResults;
	String* stations = find_station_by_name(emptyQuery);
	JSON_Value* jsonValue = string_to_json(stations);
	JSON_Object *response, *station,
			*expectResponseSchemaJSONObject = json_value_get_object(json_parse_string(expectResponseSchema));
	JSON_Array* locations;
	char* name,*place,**results;
	/*
		check the integrity of return data
		it should have the format
		{
				"locations": [
					{
						...
						"name": "...",
						...
					},
					...
				]
		}
	*/
	// parse json value to json object

	assert(jsonValue != NULL);
	assert_type(jsonValue,JSONObject);
	response = json_value_get_object(jsonValue);

	// extract location and equivalent...
	locations = json_object_get_array(response,"locations");
	assert(locations != NULL);
	// count number of stations
	numResults = json_array_get_count(locations);
	for(i = 0,numStations = 0; i < numResults; i++,numStations++)
	{
			station = json_array_get_object(locations,i);
			if (
					station == NULL
				||json_object_get_value(station,"type") == NULL
				||strcmp(json_object_get_string(station,"type"),"station") != 0)
			{
				numStations--;
				continue;
			}
	}
	// allocate size
	results = malloc(sizeof(char*) * numStations);
	assert(results != NULL);
	// start putting all stations together...
	for(int i = 0,j = 0; i < numResults; i++,j++)
	{
		station = json_array_get_object(locations,i);
		// add the name of this station
		if (
				station == NULL
			||json_object_get_value(station,"type") == NULL
			||strcmp(json_object_get_string(station,"type"),"station") != 0)
		{
			j--;
			continue;
		}
		name = json_object_get_string(station,"name");
		place = json_object_get_string(station,"place");
		assert(place != NULL && name != NULL);
		int resultLength = strlen(name) + strlen(place) + 3;
		results[j] = malloc(resultLength);
		snprintf(results[j],resultLength,"%s, %s",name,place);
	}
	// clean up
	clean_list(stations);
	json_value_free(jsonValue);
	json_value_free(expectResponseSchemaJSONObject);

	// prepare and return results
	*size = numStations;
	return results;
}
int main()
{
	int numStations;
	char** stationList = get_stations(NULL,&numStations);
	for(int i = 0; i < numStations; i++)
	{
		printf("%s\n",stationList[i]);
	}
	// clean up
	for(int i = 0; i < numStations; i++)
	{
		free(stationList[i]);
	}
	free(stationList);
	return 0;
}
