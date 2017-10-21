package mvg

import (
	// "bytes"
	"time"
	// "strings"
	"strconv"
)

type Departure struct {
	DepartureTime int64
	Product,Destination string
	Label string
	From Station
}

type Lines struct {
	Tram,Nachttram,Sbahn,Ubahn,Bus,Nachtbus,Otherlines []string
}

func (d *Departure) Departure_time() time.Time {
	return time.Unix(d.DepartureTime / 1000,0)
}

func (d *Departure) As_Lines() Lines {
	// var buffer bytes.Buffer
	line := Lines{}
	var cat *[]string
	switch d.Product {
		case "u":
				cat = &(line.Ubahn)
		case "t":
				cat = &(line.Tram)
		case "n":
				cat = &(line.Nachtbus)
		case "s":
				cat = &(line.Sbahn)
		default:
			if  _, err := strconv.Atoi(d.Product); err == nil {
				cat = &line.Bus
			} else {cat = nil}
	}
	if cat == nil {return line}
	*cat = append(*cat,d.Label)
	// buffer.WriteString(strings.ToUpper(d.Product))
	// buffer.WriteString(d.Label)
	return line
}
