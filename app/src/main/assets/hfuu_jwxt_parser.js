/**
 * 合肥大学教务处课表解析脚本 (参考 ShiGuangSchedule 适配引擎设计)
 * 支持：正方新/旧教务、强智教务、树维/金智教务，以及同源 iframe 课表表格通用解析。
 */
(function () {
  'use strict';

  function collectDocuments(win, docs) {
    docs = docs || [];
    try {
      if (win && win.document) {
        docs.push(win.document);
        var frames = win.frames || [];
        for (var i = 0; i < frames.length; i++) {
          collectDocuments(frames[i], docs);
        }
      }
    } catch (e) {
      // 忽略跨域 iframe
    }
    return docs;
  }

  function parseWeeks(text) {
    if (!text) return [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16];
    var weeks = {};
    // 先移除节次片段（如 "(5-6节)" 或 "第1-2节"），避免节次数字混入周次
    var withoutSec = text.replace(/[\(（\[]?\s*第?\s*\d+\s*[-~至,]\s*\d+\s*节\s*[\)）\]]?/g, ' ')
                         .replace(/第?\s*\d+\s*节/g, ' ');
    var cleaned = withoutSec.replace(/第|周|\[|\]|\(|\)|（|）/g, ' ');
    var regex = /(\d+)\s*[-~至]\s*(\d+)\s*(单|双)?|(\d+)/g;
    var match;
    while ((match = regex.exec(cleaned)) !== null) {
      if (match[1] && match[2]) {
        var start = parseInt(match[1], 10);
        var end = parseInt(match[2], 10);
        var parity = match[3] || '';
        for (var w = start; w <= end; w++) {
          if (parity === '单' && w % 2 === 0) continue;
          if (parity === '双' && w % 2 !== 0) continue;
          if (w >= 1 && w <= 30) weeks[w] = true;
        }
      } else if (match[4]) {
        var single = parseInt(match[4], 10);
        if (single >= 1 && single <= 30) weeks[single] = true;
      }
    }
    var result = Object.keys(weeks).map(function (k) { return parseInt(k, 10); });
    result.sort(function (a, b) { return a - b; });
    return result.length > 0 ? result : [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16];
  }

  function parseSections(text, fallbackStart, fallbackSpan) {
    // 必须显式带“节”字，防止把“5-7周”误解析成“5-7节”
    var m = (text || '').match(/(\d+)\s*[-~至,]\s*(\d+)\s*节/);
    if (m) {
      var s = parseInt(m[1], 10);
      var e = parseInt(m[2], 10);
      if (s >= 1 && e >= s && e <= 14) return [s, e];
    }
    var single = (text || '').match(/第?\s*(\d+)\s*节/);
    if (single) {
      var n = parseInt(single[1], 10);
      if (n >= 1 && n <= 14) {
        var span = fallbackSpan && fallbackSpan >= 1 && fallbackSpan <= 4 ? fallbackSpan : 2;
        return [n, Math.min(n + span - 1, 12)];
      }
    }
    var base = fallbackStart || 1;
    var defaultSpan = fallbackSpan && fallbackSpan >= 1 && fallbackSpan <= 4 ? fallbackSpan : 2;
    return [base, Math.min(base + defaultSpan - 1, 12)];
  }

  function isLikelyRoom(line) {
    if (!line) return false;
    // 兼容 36-610、jt1、JT101、37-jt2、二期jt、阶梯教室、教学楼、实验室、机房等格式（不区分大小写）
    return /楼|室|馆|场|区|栋|教|厅|阶梯|机房|实训|实验|中心|操场|体育|jt|\d+\s*[-－—]\s*[A-Za-z0-9\u4e00-\u9fa5]+|[A-Za-z]{1,4}\s*\d{1,4}|\d{3}/i.test(line);
  }

  function cleanRoomText(text) {
    return (text || '')
      .replace(/^上课地点[：:\s]*|^地点[：:\s]*|^教室[：:\s]*/, '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  function extractStudentInfo(docs) {
    var info = { name: '', studentId: '', className: '', major: '' };
    for (var i = 0; i < docs.length; i++) {
      var bodyText = (docs[i].body && docs[i].body.innerText) ? docs[i].body.innerText : '';
      if (!info.studentId) {
        var idM = bodyText.match(/学号[：:\s]*([0-9A-Za-z]{6,16})/);
        if (idM) info.studentId = idM[1].trim();
      }
      if (!info.name) {
        var nameM = bodyText.match(/姓名[：:\s]*([\u4e00-\u9fa5]{2,5})/);
        if (nameM) info.name = nameM[1].trim();
      }
      if (!info.className) {
        var clsM = bodyText.match(/班级[：:\s]*([^\s\n，,]{2,20})/);
        if (clsM) info.className = clsM[1].trim();
      }
      if (!info.major) {
        var majM = bodyText.match(/专业[：:\s]*([^\s\n，,]{2,20})/);
        if (majM) info.major = majM[1].trim();
      }
    }
    return info;
  }

  // 1. 正方新教务系统解析 (table#kbgrid_table_0 / div.timetable_con)
  function parseZhengfangNew(doc) {
    var courses = [];
    var blocks = doc.querySelectorAll('.timetable_con');
    if (!blocks || blocks.length === 0) return courses;

    for (var i = 0; i < blocks.length; i++) {
      var block = blocks[i];
      var td = block.closest('td');
      var tdId = td ? (td.getAttribute('id') || '') : '';
      var rowSpan = td ? parseInt(td.getAttribute('rowspan') || '0', 10) : 0;
      var day = 1;
      var fallbackSection = 1;
      var idParts = tdId.split('-');
      if (idParts.length >= 2) {
        day = parseInt(idParts[0], 10) || 1;
        fallbackSection = parseInt(idParts[1], 10) || 1;
      }

      var titleEl = block.querySelector('.title') || block.querySelector('span.title') || block.querySelector('span');
      var name = titleEl ? titleEl.innerText.trim() : '';
      var text = block.innerText || '';
      var lines = text.split('\n').map(function (s) { return s.trim(); }).filter(Boolean);
      if (!name && lines.length > 0) name = lines[0];

      var weekLine = '';
      var room = '';
      var teacher = '';

      // 优先通过正方自带的图标/title 属性精准提取地点、节周与教师（完美识别 jt 阶梯教室）
      var paragraphs = block.querySelectorAll('p');
      for (var p = 0; p < paragraphs.length; p++) {
        var pEl = paragraphs[p];
        var pText = (pEl.innerText || '').trim();
        if (!pText) continue;
        if (pEl.querySelector('.glyphicon-map-marker, span[title*="地点"], span[title*="教室"], font[title*="教室"]')) {
          if (!room) room = cleanRoomText(pText);
        } else if (pEl.querySelector('.glyphicon-time, span[title*="节"], span[title*="周"]')) {
          weekLine += ' ' + pText;
        } else if (pEl.querySelector('.glyphicon-user, span[title*="教师"], span[title*="老师"]')) {
          if (!teacher) teacher = pText.replace(/^教师[：:\s]*|^老师[：:\s]*/, '').trim();
        }
      }

      // 兜底：逐行识别
      for (var j = 0; j < lines.length; j++) {
        var line = lines[j];
        if (line === name) continue;
        if (line.indexOf('周') !== -1 || line.indexOf('节') !== -1) {
          if (weekLine.indexOf(line) === -1) weekLine += ' ' + line;
        } else if (!room && isLikelyRoom(line)) {
          room = cleanRoomText(line);
        } else if (!teacher && /^[\u4e00-\u9fa5]{2,5}(?:[、,，][\u4e00-\u9fa5]{2,5})*$/.test(line)) {
          teacher = line;
        }
      }

      var sec = parseSections(weekLine, fallbackSection, rowSpan);
      if (name) {
        courses.push({
          name: name.replace(/【.*?】/g, '').trim(),
          teacher: teacher || '未知教师',
          location: room || '待定教室',
          dayOfWeek: Math.min(Math.max(day, 1), 7),
          startSection: sec[0],
          endSection: sec[1],
          weeks: parseWeeks(weekLine)
        });
      }
    }
    return courses;
  }

  // 2. 强智教务系统解析 (table#kbtable / div.kbcontent)
  function parseQiangzhi(doc) {
    var courses = [];
    var cells = doc.querySelectorAll('div.kbcontent');
    if (!cells || cells.length === 0) return courses;

    for (var i = 0; i < cells.length; i++) {
      var cell = cells[i];
      var td = cell.closest('td');
      var tr = cell.closest('tr');
      var day = td ? td.cellIndex : 1;
      var rowIndex = tr ? tr.rowIndex : 1;
      var fallbackStart = Math.max(1, (rowIndex - 1) * 2 + 1);

      var htmlParts = cell.innerHTML.split(/-----+|<br\s*\/?>\s*<br\s*\/?>/i);
      for (var p = 0; p < htmlParts.length; p++) {
        var tmp = doc.createElement('div');
        tmp.innerHTML = htmlParts[p];
        var lines = (tmp.innerText || '').split('\n').map(function (s) { return s.trim(); }).filter(Boolean);
        if (lines.length < 2) continue;

        var name = lines[0];
        var teacherEl = tmp.querySelector('font[title*="老师"], font[title*="教师"]');
        var weekEl = tmp.querySelector('font[title*="周次"]');
        var roomEl = tmp.querySelector('font[title*="教室"], font[title*="地点"]');

        var teacher = teacherEl ? teacherEl.innerText.trim() : '';
        var weekStr = weekEl ? weekEl.innerText.trim() : '';
        var room = roomEl ? cleanRoomText(roomEl.innerText) : '';

        for (var k = 1; k < lines.length; k++) {
          var l = lines[k];
          if (!weekStr && (l.indexOf('周') !== -1 || l.indexOf('节') !== -1)) {
            weekStr = l;
          } else if (!room && isLikelyRoom(l)) {
            room = cleanRoomText(l);
          } else if (!teacher && l !== room && l !== weekStr) {
            teacher = l;
          }
        }

        var sec = parseSections(weekStr, fallbackStart, 2);

        if (name) {
          courses.push({
            name: name,
            teacher: teacher || '未知教师',
            location: room || '待定教室',
            dayOfWeek: Math.min(Math.max(day, 1), 7),
            startSection: sec[0],
            endSection: sec[1],
            weeks: parseWeeks(weekStr)
          });
        }
      }
    }
    return courses;
  }

  // 3. 通用课表表格解析（兼容树维/金智/老教务）
  function parseGenericTable(doc) {
    var courses = [];
    var tables = doc.querySelectorAll('table');
    for (var t = 0; t < tables.length; t++) {
      var table = tables[t];
      var headerText = table.innerText || '';
      if (headerText.indexOf('星期一') === -1 && headerText.indexOf('周一') === -1) continue;

      var rows = table.querySelectorAll('tr');
      var dayColMap = {};
      var dayKeywords = ['一', '二', '三', '四', '五', '六', '日'];

      for (var r = 0; r < rows.length; r++) {
        var cells = rows[r].querySelectorAll('th, td');
        if (Object.keys(dayColMap).length === 0) {
          for (var c = 0; c < cells.length; c++) {
            var txt = (cells[c].innerText || '').trim();
            for (var d = 0; d < dayKeywords.length; d++) {
              if (txt.indexOf('星期' + dayKeywords[d]) !== -1 || txt.indexOf('周' + dayKeywords[d]) !== -1) {
                dayColMap[c] = d + 1;
              }
            }
          }
          continue;
        }

        for (var c2 = 0; c2 < cells.length; c2++) {
          var day = dayColMap[c2];
          if (!day) continue;
          var cellText = (cells[c2].innerText || '').trim();
          if (cellText.length < 3) continue;

          var blocks = cellText.split(/\n{2,}|-{4,}/);
          for (var b = 0; b < blocks.length; b++) {
            var lines = blocks[b].split('\n').map(function (s) { return s.trim(); }).filter(Boolean);
            if (lines.length === 0) continue;
            var name = lines[0];
            if (/^(第?\d+节|上午|下午|晚上|午休)$/.test(name)) continue;

            var joined = lines.join(' ');
            var rowSpan = parseInt(cells[c2].getAttribute('rowspan') || '2', 10);
            var startSec = Math.max(1, r);
            var sec = parseSections(joined, startSec, rowSpan);

            var location = '';
            var teacher = '';
            for (var k = 1; k < lines.length; k++) {
              if (lines[k].indexOf('周') !== -1 || lines[k].indexOf('节') !== -1) continue;
              if (!location && isLikelyRoom(lines[k])) {
                location = cleanRoomText(lines[k]);
              } else if (!teacher) {
                teacher = lines[k];
              }
            }

            courses.push({
              name: name,
              teacher: teacher || '未知教师',
              location: location || '待定教室',
              dayOfWeek: day,
              startSection: sec[0],
              endSection: sec[1],
              weeks: parseWeeks(joined)
            });
          }
        }
      }
      if (courses.length > 0) break;
    }
    return courses;
  }

  var docs = collectDocuments(window, []);
  var allCourses = [];
  var source = 'unknown';

  for (var i = 0; i < docs.length; i++) {
    var zf = parseZhengfangNew(docs[i]);
    if (zf.length > 0) {
      allCourses = zf;
      source = 'zhengfang_new';
      break;
    }
    var qz = parseQiangzhi(docs[i]);
    if (qz.length > 0) {
      allCourses = qz;
      source = 'qiangzhi';
      break;
    }
  }

  if (allCourses.length === 0) {
    for (var j = 0; j < docs.length; j++) {
      var gen = parseGenericTable(docs[j]);
      if (gen.length > 0) {
        allCourses = gen;
        source = 'generic_table';
        break;
      }
    }
  }

  var payload = {
    success: allCourses.length > 0,
    source: source,
    url: window.location.href,
    studentInfo: extractStudentInfo(docs),
    courses: allCourses
  };

  if (window.HfuuBridge && typeof window.HfuuBridge.onScheduleParsed === 'function') {
    window.HfuuBridge.onScheduleParsed(JSON.stringify(payload));
  }
  return JSON.stringify(payload);
})();
