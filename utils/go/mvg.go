package main

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"encoding/json"
)

type Lines struct {
	Tram,Nachttram,Sbahn,Ubahn,Bus,Nachtbus,Otherlines []string
}

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
	url := fmt.Sprintf("https://www.mvg.de/fahrinfo/api/location/queryWeb?q=%s",searchWord)
	return get_station_results(url)
}

func get_all_stations() ([]Station,error) {
	return search_stations("")
}
func find_closest_station(lat float64, lon float64) ([]Station,error) {
	url := fmt.Sprintf("https://www.mvg.de/fahrinfo/api/location/nearby?latitude=%5f&longitude=%5f",lat,lon)
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

func main() {
		stations,err := stations_nearby("Hauptbahnhof")
		if err != nil {
				fmt.Println(err)
				return
		}
		fmt.Printf("Number of stations: %d\n",len(stations))
		fmt.Println(stations)
}
