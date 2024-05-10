import React from 'react';
import ReactMapGL, { Source, Layer } from 'react-map-gl';
import osmtogeojson from 'osmtogeojson';
import mapData from './map.osm'; 

const Map = () => {
  const [viewport, setViewport] = React.useState({
    latitude: 51.505,
    longitude: -0.09,
    zoom: 13,
    width: '100%',
    height: '100%',
  });

  const geojsonData = osmtogeojson(mapData); 

  return (
    <div className="map-block">
      <div className="map-container">
        <ReactMapGL
          {...viewport}
          mapboxApiAccessToken={process.env.REACT_APP_MAPBOX_TOKEN}
          onViewportChange={(newViewport) => setViewport(newViewport)}
          mapStyle="mapbox://styles/mapbox/streets-v11"
        >
          <Source type="geojson" data={geojsonData}>
            <Layer type="line" paint={{ 'line-color': '#FF0000', 'line-width': 2 }} />
          </Source>
        </ReactMapGL>
      </div>
    </div>
  );
};

export default Map;