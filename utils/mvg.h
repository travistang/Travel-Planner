#include <curl/curl.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include "expandable_list.c"
#include "external/jsmn/jsmn.h"
typedef ExpandableList String;
typedef struct curl_slist Header;

String* init_string();
static size_t write_func(void *ptr,size_t size,size_t nmemb,void* s);
CURLcode get(const char* url,String* container,Header* header);
Header* get_mvg_header();

// fetch permanent data
// server options
typedef struct{
	char* appid;
	char* master_key;
	char* url;
}ParseServerOpt;

#define STATION "station"

void update_station_list();
String* find_station_by_name(char* searchWord);
String* find_closest_station(float lat, float log);

// common usage
// TODO: more functions...
char** get_stations(ParseServerOpt* opt);
