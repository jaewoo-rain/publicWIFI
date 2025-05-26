const express = require('express');
const mysql = require('mysql2');
const cors = require('cors');

const app = express();
const port = 5000;

// 미들웨어 설정
app.use(cors());

// MySQL 연결 설정
const db = mysql.createConnection({
  host: 'host.docker.internal', // ← 요거!
  port: 33307,
  user: 'jaewoo',
  password: 'jaewoo',
  database: 'docker_mysql'
});

// MySQL 연결
db.connect((err) => {
  if (err) {
    console.error('MySQL 연결 실패:', err.message);
    process.exit(1);
  }
  console.log('MySQL 연결 성공');
});

// JSON 데이터 반환 라우트
app.get('/wifi', (req, res) => {
  const sql = 'SELECT DISTINCT * FROM wifi';

  db.query(sql, (err, results) => {
    if (err) {
      console.error('쿼리 실행 오류:', err.message);
      return res.status(500).json({ error: 'DB 오류: ' + err.message });
    }

    const data = results.map(row => ({
      id: row.id,
      name: row.wifi_SSID,
      address: row.address,
      center: row.manager_center,
      contact: row.manager_phone,
      latitude: row.latitude,
      longitude: row.longitude

    }));

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.json({ wifi: data });
  });
});

// 서버 실행
app.listen(port, () => {
  console.log(`서버 실행 중: http://localhost:${port}`);
});
