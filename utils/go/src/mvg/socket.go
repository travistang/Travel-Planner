package mvg
/*
	Functions for interacting with the MVG-live API
*/
import (
	"fmt"
	"io/ioutil"
	"net/http"
	"encoding/json"
	"net/url"
	"strconv"
//	"time"
)

type Station struct {
	Type,Place, Name,Aliases string
	Latitude,Longitude float64
	Id int
	Products []string
	Line Lines
}

// return parsed json or None
func request_mvg(url string) ([]byte,error) {
	// Request (GET https://www.mvg.de/fahrinfo/api/location/queryWeb?q)

	// Create client
	client := &http.Client{}

	// Create request
	req, err := http.NewRequest("GET", url, nil)

	// Headers
	req.Header.Add("X-MVG-Authorization-Key", "5af1beca494712ed38d313714d4caff6")

	parseFormErr := req.ParseForm()
	if parseFormErr != nil {
	  fmt.Println(parseFormErr)
	}

	// Fetch Request
	resp, err := client.Do(req)

	if err != nil {
		fmt.Println("Failure : ", err)
	}

	// Read Response Body
	return ioutil.ReadAll(resp.Body)
}

func get_station_results(url string) ([]Station, error){
	jsonString,err := request_mvg(url)
	if err != nil {
		return nil,err
	}
	// load the response
	var jsonResponse map[string][]Station
	if err = json.Unmarshal(jsonString,&jsonResponse); err != nil {
		return nil,err
	}
	return jsonResponse["locations"],nil
}
func search_stations(searchWord string) ([]Station,error) {
	// construct search string
	form := url.Values{}
	form.Add("q",searchWord)
	searchString := form.Encode()
	// construct URL
	url := fmt.Sprintf(
		"https://www.mvg.de/fahrinfo/api/location/queryWeb?%s",searchString)
	return get_station_results(url)
}

func get_all_stations() ([]Station,error) {
	return search_stations("")
}
func find_closest_station(lat float64, lon float64) ([]Station,error) {
	url := fmt.Sprintf(
		"https://www.mvg.de/fahrinfo/api/location/nearby?latitude=%5f&longitude=%5f",
		lat,lon)
	return get_station_results(url)
}

func stations_nearby(stationName string) ([]Station,error) {
	stations,err := search_stations(stationName)
	if err != nil {
		return nil, err
	}
	// check the return stations itself
	if len(stations) == 0 {
		return make([]Station,0), nil
	}
	// chose the first one and retrieve the coordinates
	lat,log := stations[0].Latitude,stations[0].Longitude
	return find_closest_station(lat,log)
}

func departure_from(station Station) ([]Departure,error){
	url := fmt.Sprintf("https://www.mvg.de/fahrinfo/api/departure/%d",station.Id)
	jsonString,err := request_mvg(url)
	if err != nil {
		return nil,err
	}
	// load the response
	var jsonResponse map[string][]Departure
	if err = json.Unmarshal(jsonString,&jsonResponse); err != nil {
		return nil,err
	}
	return jsonResponse["departures"],nil
}
// TODO: take time into consideration
// TODO: use a struct of request type instead of what we have now...
func TestMain() {
	routes, err := GetRoute("Woferlstraße","Alte Heide",10,10)
	if err != nil {
		fmt.Println(err)
	}
	r := routes[0]
	fmt.Println(r.NumChange())
	fmt.Println(r.TotalWaitingTime())
}

func get_route(fromStation string, toStation string,maxTravelTimeFootwayToStation int, maxTravelTimeFootwayToDestination int) ([]Route,error) {
	//Retrieve stations
	fromStationObjs, err := search_stations(fromStation)
	fromStationObj := fromStationObjs[0]
	if err != nil {
		return nil,err
	}

	toStationObjs, err := search_stations(toStation)
	toStationObj := toStationObjs[0]
	if err != nil {
		return nil,err
	}
	// then their Ids
	fromId,toId := fromStationObj.Id,toStationObj.Id

	// construct URL query string
	params := url.Values{}
	params.Add("fromStation",strconv.Itoa(fromId))
	params.Add("toStation",strconv.Itoa(toId))
	params.Add("maxTravelTimeFootwayToStation",strconv.Itoa(maxTravelTimeFootwayToStation))
	params.Add("maxTravelTimeFootwayToDestination",strconv.Itoa(maxTravelTimeFootwayToDestination))
	// then construct the request URLs
	url := fmt.Sprintf("https://www.mvg.de/fahrinfo/api/routing/?%s",params.Encode())
	// request and get response
	jsonString,err := request_mvg(url)
	// parse the result
	
	var jsonResponse map[string][]Route
	if err = json.Unmarshal(jsonString,&jsonResponse); err != nil {
		return nil,err
	}
	return jsonResponse["connectionList"],nil
}
