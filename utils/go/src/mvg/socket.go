package mvg
/*
	Functions for interacting with the MVG-live API
*/
import (
	"fmt"
	"io/ioutil"
	"net/http"
	"encoding/json"
	"time"
)


type Coordinate struct {
	Latitude,Longitude float64
}

type Station struct {
	Type,Place, Name,Aliases string
	Latitude,Longitude float64
	Id int
	Products []string
	Line Lines
}

type Route struct {
	StartTime,ArrivalTime time.Time
	Lines
	ringFrom,ringTo int
	fromStation,toStation Station
	next *Route
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
	url := fmt.Sprintf(
		"https://www.mvg.de/fahrinfo/api/location/queryWeb?q=%s",searchWord)
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

func TestMain() {

		stations,err := stations_nearby("Hauptbahnhof")
		if err != nil {
				fmt.Println(err)
				return
		}
		fmt.Println(stations[0])
		departures,err := departure_from(stations[0])

		if err != nil {
				fmt.Println(err)
				return
		}
		fmt.Println(departures[0].Departure_time())
		fmt.Println(departures[0].As_Lines())
}
