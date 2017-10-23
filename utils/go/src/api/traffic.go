package api

import (
	"encoding/json"
	"net/http"
	"github.com/gorilla/mux"
	"log"
	"fmt"

	"mvg"
)

type Endpoint struct {
	Endpoint,Method string
	Handler func(http.ResponseWriter,*http.Request)
}
type Response struct {
	ok int
	response interface{}
}
func (r *Response) AsMap() map[string]interface{} {
	res := make(map[string]interface{})
	res["ok"] = r.ok
	res["response"] = r.response
	return res
}
// handlers
func departure_handler(w http.ResponseWriter,request *http.Request) {
	params := mux.Vars(request)
	w.Header().Set("Content-Type","application/json")
	if station,ok := params["station"]; ok {
		stationObj,err := mvg.FindStationByName(station)
		if err != nil || len(stationObj) == 0 {
			errorResponse := Response {ok: 0,response: fmt.Sprintf("No such station: %s",station)}
			json.NewEncoder(w).Encode(errorResponse.AsMap())
			return
		}
		// obtain departures
		departures,err := mvg.DepartureFrom(stationObj[0])
		fmt.Println(len(departures))
		if err != nil {
			errorResponse := Response {ok: 0,response: fmt.Sprintf("%+v",err)}
			json.NewEncoder(w).Encode(errorResponse.AsMap())
			return
		}
		// return departures
		resp := Response{ok: 1, response: departures}
		json.NewEncoder(w).Encode(resp.AsMap())
		fmt.Println("hiii")
		return
	}
	// no departure param, return error
	errorResponse := Response {
		ok: 0,
		response: "Missing parameter `station`"}
	json.NewEncoder(w).Encode(errorResponse.AsMap())
}
func TestAPI() {
	endpoints := []Endpoint{
		Endpoint{
			// This is a test...
			Endpoint : "/mvg/departure/{station}",
			Method : "GET",
			Handler: departure_handler},

		// TODO: correct me
		Endpoint{
			Endpoint: "/mvg/route",
			Method: "GET",
			Handler: departure_handler}}

	RunMVGApi(":12333",endpoints)
}

func RunMVGApi(port string,endpoints []Endpoint) {
	router := mux.NewRouter()
	for _,endpoint := range endpoints {
		router.HandleFunc(endpoint.Endpoint,endpoint.Handler).Methods(endpoint.Method)
	}

	log.Fatal(http.ListenAndServe(port, router))
}
