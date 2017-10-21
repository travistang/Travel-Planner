package mvg
import ()
type Station struct {
	Type,Place, Name,Aliases string
	Latitude,Longitude float64
	Id int
	Products []string
	Line Lines
}

func (s* Station) GetDepartures() ([]Departure,error) {
	return departure_from(*s)
}

