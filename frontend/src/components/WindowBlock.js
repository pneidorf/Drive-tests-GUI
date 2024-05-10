import React, { useState } from 'react';

function WindowBlock() {
  
  const [activeTab, setActiveTab] = useState('plots');

  const handleTabClick = (tab) => {
    setActiveTab(tab);
  };

  return (
    <div className="window-block">
      <div className="tabs-container">
        <div className="tabs">
          <button className='shadow-gradient-button' onClick={() => handleTabClick('plots')}>Plots</button>
          <button className='shadow-gradient-button' onClick={() => handleTabClick('map')}>Map</button>
          <button className='shadow-gradient-button' onClick={() => handleTabClick('sin')}>Sin</button>
        </div>
      </div>
      <div className='map-block'>
        <div className="map-container">
          {activeTab === 'plots' && <div>Графики</div>}
          {activeTab === 'map' && <div>Карта</div>}
          {activeTab === 'sin' && <div>Пустая страница</div>}
        </div>
      </div>
    </div>
  );
}


export default WindowBlock;