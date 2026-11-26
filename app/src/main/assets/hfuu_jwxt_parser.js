/**
 * 合肥大学教务处课表解析脚本 (参考 ShiGuangSchedule 适配引擎设计)
 * 支持：正方新/旧教务、强智教务、树维/金智教务，以及同源 iframe 课表表格通用解析。
 * v2 修复：
 * 1. 保留同课程不同周次对应的不同教室（如理论 35-208 与实验/换教室 36-204 独立提取）；
 * 2. 正方优先解析网格表 #kbgrid_table_0 / td[id="周-节"]，避免列表视图串位；
 * 3. 强智兼容隐藏节点 .kbcontent，使用 textContent + <br> 换行替换避免 detached innerText 丢换行；
 * 4. 通用/强智表格引入完整 2D rowspan/colspan 占位矩阵，彻底杜绝列偏移串教室。
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

  function htmlToLines(doc, html) {
    if (!html) return [];
    var prepared = html
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/(p|div|li|tr)>/gi, '\n')
      .replace(/<hr[^>]*>/gi, '\n-----\n');
    var tmp = doc.createElement('div');
    tmp.innerHTML = prepared;
    var raw = tmp.textContent || tmp.innerText || '';
    return raw.split('\n').map(function (s) {
      return s.replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
    }).filter(Boolean);
  }

  function parseWeeks(text) {
    if (!text) return [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16];
    var weeks = {};
    // 1. 移除节次片段（如 "(5-6节)"、"第1-2节"、"[01-02节]"）
    var withoutSec = text
      .replace(/[\(（\[]?\s*第?\s*\d+\s*[-~至,]\s*\d+\s*节\s*[\)）\]]?/g, ' ')
      .replace(/第?\s*\d+\s*节/g, ' ');
    // 2. 移除教室编号片段（如 36-204、35-208、36-2JT、12-301），防止建筑楼号混入周次
    withoutSec = withoutSec.replace(/\b\d+\s*[-－—]\s*(?:\d{3,}|[A-Za-z]+\d*|\d+[A-Za-z]+)\b/gi, ' ');

    // 支持前置单双周标记，如 "(单)1-16周" / "单周1-16"
    var globalParity = '';
    if (/^\s*[\(（\[]?\s*单周?\s*[\)）\]]?/.test(withoutSec)) globalParity = '单';
    else if (/^\s*[\(（\[]?\s*双周?\s*[\)）\]]?/.test(withoutSec)) globalParity = '双';

    // 逐段按逗号/分号拆分，确保 "(单)" / "(双)" 只作用于对应的周次区间
    var segments = withoutSec.split(/[,，;；]+/);
    for (var sIdx = 0; sIdx < segments.length; sIdx++) {
      var seg = segments[sIdx];
      var segParity = globalParity;
      if (/单/.test(seg)) segParity = '单';
      else if (/双/.test(seg)) segParity = '双';

      var cleaned = seg.replace(/第|周|单|双|\[|\]|\(|\)|（|）/g, ' ');
      var regex = /(\d+)\s*[-~至]\s*(\d+)|(\d+)/g;
      var match;
      while ((match = regex.exec(cleaned)) !== null) {
        if (match[1] && match[2]) {
          var start = parseInt(match[1], 10);
          var end = parseInt(match[2], 10);
          if (start >= 1 && end >= start && end <= 30) {
            for (var w = start; w <= end; w++) {
              if (segParity === '单' && w % 2 === 0) continue;
              if (segParity === '双' && w % 2 !== 0) continue;
              weeks[w] = true;
            }
          }
        } else if (match[3]) {
          var single = parseInt(match[3], 10);
          if (single >= 1 && single <= 30) weeks[single] = true;
        }
      }
    }
    var result = Object.keys(weeks).map(function (k) { return parseInt(k, 10); });
    result.sort(function (a, b) { return a - b; });
    return result.length > 0 ? result : [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16];
  }

  function parseSections(text, fallbackStart, fallbackSpan) {
    // 必须显式带“节”字，防止把“5-7周”误解析成“5-7节”
    var m = (text || '').match(/0?(\d+)\s*[-~至,]\s*0?(\d+)\s*节/);
    if (m) {
      var s = parseInt(m[1], 10);
      var e = parseInt(m[2], 10);
      if (s >= 1 && e >= s && e <= 14) return [s, e];
    }
    var single = (text || '').match(/第?\s*0?(\d+)\s*节/);
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
    if (/^\d+\s*[-~至]\s*\d+\s*周/.test(line)) return false;
    // 兼容 36-204、35-208、36-2JT、jt1、JT101、37-jt2、二期jt、阶梯教室、教学楼、实验室、机房等格式
    return /楼|室|馆|场|区|栋|教|厅|阶梯|机房|实训|实验|中心|操场|体育|jt|\d+\s*[-－—]\s*[A-Za-z0-9\u4e00-\u9fa5]+|[A-Za-z]{1,4}\s*\d{1,4}|\d{3}/i.test(line);
  }

  function cleanRoomText(text) {
    return (text || '')
      .replace(/\u00a0/g, ' ')
      .replace(/^上课地点[：:\s]*|^教学地点[：:\s]*|^地点[：:\s]*|^教室[：:\s]*|^场地[：:\s]*/i, '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  function cleanTeacherText(text) {
    return (text || '')
      .replace(/\u00a0/g, ' ')
      .replace(/^任课教师[：:\s]*|^教师[：:\s]*|^老师[：:\s]*/i, '')
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

  /**
   * 计算表格的完整 2D 网格映射（严格处理 rowspan 和 colspan），
   * 返回数组：每项包含 { cell, row, col, rowSpan, colSpan }
   */
  function buildTableMatrix(table) {
    var rows = table.querySelectorAll('tr');
    var occupied = [];
    var entries = [];
    for (var r = 0; r < rows.length; r++) {
      if (!occupied[r]) occupied[r] = [];
      var cells = rows[r].children;
      var col = 0;
      for (var c = 0; c < cells.length; c++) {
        var tag = (cells[c].tagName || '').toUpperCase();
        if (tag !== 'TD' && tag !== 'TH') continue;
        while (occupied[r][col]) col++;
        var rs = parseInt(cells[c].getAttribute('rowspan') || '1', 10) || 1;
        var cs = parseInt(cells[c].getAttribute('colspan') || '1', 10) || 1;
        for (var dr = 0; dr < rs; dr++) {
          if (!occupied[r + dr]) occupied[r + dr] = [];
          for (var dc = 0; dc < cs; dc++) {
            occupied[r + dr][col + dc] = true;
          }
        }
        entries.push({ cell: cells[c], row: r, col: col, rowSpan: rs, colSpan: cs });
        col += cs;
      }
    }
    return { rowCount: rows.length, entries: entries };
  }

  // 1. 正方新教务系统解析 (table#kbgrid_table_0 / div.timetable_con)
  function parseZhengfangNew(doc) {
    var courses = [];
    // 优先仅从网格课表 #kbgrid_table_0 或带 "周-节" td[id] 的单元格提取，避免把隐藏的列表视图 #kblist_table 混进来串位
    var gridBlocks = doc.querySelectorAll('#kbgrid_table_0 .timetable_con');
    var blocks = [];
    if (gridBlocks && gridBlocks.length > 0) {
      blocks = Array.prototype.slice.call(gridBlocks);
    } else {
      var allCon = doc.querySelectorAll('.timetable_con');
      for (var a = 0; a < allCon.length; a++) {
        var tdCheck = allCon[a].closest('td');
        var idCheck = tdCheck ? (tdCheck.getAttribute('id') || '') : '';
        if (/^\d+-\d+$/.test(idCheck)) {
          blocks.push(allCon[a]);
        }
      }
      if (blocks.length === 0 && allCon && allCon.length > 0) {
        blocks = Array.prototype.slice.call(allCon);
      }
    }
    if (blocks.length === 0) return courses;

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
      var name = titleEl ? (titleEl.textContent || titleEl.innerText || '').trim() : '';
      var lines = htmlToLines(doc, block.innerHTML);
      if (!name && lines.length > 0) name = lines[0];

      var weekLine = '';
      var room = '';
      var teacher = '';

      // 逐个段落/元素独立提取地点、周节次、教师（避免同一 <p> 内含多个 icon/span 时 else-if 漏读）
      var paragraphs = block.querySelectorAll('p');
      for (var p = 0; p < paragraphs.length; p++) {
        var pEl = paragraphs[p];
        var pText = (pEl.textContent || pEl.innerText || '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
        if (!pText) continue;

        var hasMap = !!pEl.querySelector('.glyphicon-map-marker, span[title*="地点"], span[title*="教室"], span[title*="场地"], font[title*="教室"]');
        var hasTime = !!pEl.querySelector('.glyphicon-time, span[title*="节"], span[title*="周"]');
        var hasUser = !!pEl.querySelector('.glyphicon-user, span[title*="教师"], span[title*="老师"]');

        if (hasMap && !hasTime && !hasUser) {
          if (!room) room = cleanRoomText(pText);
        } else if (hasTime && !hasMap && !hasUser) {
          weekLine += ' ' + pText;
        } else if (hasUser && !hasMap && !hasTime) {
          if (!teacher) teacher = cleanTeacherText(pText);
        } else {
          // 同一 <p> 里包含多个属性时，尝试按子元素/文本分别读取
          if (hasMap && !room) {
            var mapEl = pEl.querySelector('span[title*="地点"], span[title*="教室"], span[title*="场地"], font[title*="教室"]');
            if (mapEl) {
              var mapTxt = (mapEl.textContent || '').trim();
              if (!mapTxt && mapEl.nextSibling) mapTxt = (mapEl.nextSibling.textContent || '').trim();
              if (mapTxt) room = cleanRoomText(mapTxt);
            }
          }
          if (hasTime) {
            var timeEl = pEl.querySelector('span[title*="节"], span[title*="周"]');
            if (timeEl) {
              var timeTxt = (timeEl.textContent || '').trim();
              if (!timeTxt && timeEl.nextSibling) timeTxt = (timeEl.nextSibling.textContent || '').trim();
              if (timeTxt) weekLine += ' ' + timeTxt;
            }
          }
          if (hasUser && !teacher) {
            var userEl = pEl.querySelector('span[title*="教师"], span[title*="老师"]');
            if (userEl) {
              var userTxt = (userEl.textContent || '').trim();
              if (!userTxt && userEl.nextSibling) userTxt = (userEl.nextSibling.textContent || '').trim();
              if (userTxt) teacher = cleanTeacherText(userTxt);
            }
          }
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
    var table = doc.querySelector('#kbtable') || (doc.querySelector('div.kbcontent') ? doc.querySelector('div.kbcontent').closest('table') : null);
    if (!table) return courses;

    var matrix = buildTableMatrix(table);
    var dayColMap = {};
    var dayKeywords = ['一', '二', '三', '四', '五', '六', '日'];
    var headerRow = -1;

    for (var e = 0; e < matrix.entries.length; e++) {
      var ent = matrix.entries[e];
      var txt = (ent.cell.textContent || '').trim();
      for (var d = 0; d < dayKeywords.length; d++) {
        if (txt.indexOf('星期' + dayKeywords[d]) !== -1 || txt.indexOf('周' + dayKeywords[d]) !== -1) {
          dayColMap[ent.col] = d + 1;
          headerRow = ent.row;
        }
      }
    }

    for (var i = 0; i < matrix.entries.length; i++) {
      var item = matrix.entries[i];
      if (headerRow >= 0 && item.row <= headerRow) continue;
      var cell = item.cell.querySelector('div.kbcontent');
      if (!cell) continue;

      var day = dayColMap[item.col] || Math.min(Math.max(item.col, 1), 7);
      var relRow = headerRow >= 0 ? (item.row - headerRow) : item.row;
      var fallbackStart = Math.max(1, (relRow - 1) * 2 + 1);

      var htmlParts = cell.innerHTML.split(/-----+|<hr[^>]*>|<br\s*\/?>\s*<br\s*\/?>/i);
      for (var p = 0; p < htmlParts.length; p++) {
        var partHtml = htmlParts[p];
        if (!partHtml || !partHtml.trim()) continue;
        var tmp = doc.createElement('div');
        tmp.innerHTML = partHtml;
        var lines = htmlToLines(doc, partHtml);
        if (lines.length < 2) continue;

        var name = lines[0];
        var teacherEl = tmp.querySelector('font[title*="老师"], font[title*="教师"]');
        var weekEls = tmp.querySelectorAll('font[title*="周次"], font[title*="节次"]');
        var roomEls = tmp.querySelectorAll('font[title*="教室"], font[title*="地点"]');

        // 若同一块内含多个周次-教室配对（未用 ----- 分隔），逐对拆分成独立排课
        if (roomEls.length > 1 && weekEls.length >= roomEls.length) {
          var sharedTeacher = teacherEl ? cleanTeacherText(teacherEl.textContent) : '';
          for (var rp = 0; rp < roomEls.length; rp++) {
            var rWeekStr = (weekEls[rp].textContent || '').trim();
            var rRoom = cleanRoomText(roomEls[rp].textContent);
            var rSec = parseSections(rWeekStr, fallbackStart, 2);
            courses.push({
              name: name.replace(/【.*?】/g, '').trim(),
              teacher: sharedTeacher || '未知教师',
              location: rRoom || '待定教室',
              dayOfWeek: Math.min(Math.max(day, 1), 7),
              startSection: rSec[0],
              endSection: rSec[1],
              weeks: parseWeeks(rWeekStr)
            });
          }
          continue;
        }

        var teacher = teacherEl ? cleanTeacherText(teacherEl.textContent) : '';
        var weekStr = weekEls.length > 0 ? (weekEls[0].textContent || '').trim() : '';
        var room = roomEls.length > 0 ? cleanRoomText(roomEls[0].textContent) : '';

        for (var k = 1; k < lines.length; k++) {
          var l = lines[k];
          if (!weekStr && (l.indexOf('周') !== -1 || l.indexOf('节') !== -1)) {
            weekStr = l;
          } else if (!room && isLikelyRoom(l)) {
            room = cleanRoomText(l);
          } else if (!teacher && l !== room && l !== weekStr) {
            teacher = cleanTeacherText(l);
          }
        }

        var sec = parseSections(weekStr, fallbackStart, 2);

        if (name) {
          courses.push({
            name: name.replace(/【.*?】/g, '').trim(),
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

  // 3. 通用课表表格解析（完整 2D rowspan/colspan 映射，兼容树维/金智/老教务）
  function parseGenericTable(doc) {
    var courses = [];
    var tables = doc.querySelectorAll('table');
    for (var t = 0; t < tables.length; t++) {
      var table = tables[t];
      var headerText = table.textContent || '';
      if (headerText.indexOf('星期一') === -1 && headerText.indexOf('周一') === -1) continue;

      var matrix = buildTableMatrix(table);
      var dayColMap = {};
      var dayKeywords = ['一', '二', '三', '四', '五', '六', '日'];
      var headerRow = -1;

      for (var h = 0; h < matrix.entries.length; h++) {
        var hent = matrix.entries[h];
        var htxt = (hent.cell.textContent || '').trim();
        for (var d = 0; d < dayKeywords.length; d++) {
          if (htxt.indexOf('星期' + dayKeywords[d]) !== -1 || htxt.indexOf('周' + dayKeywords[d]) !== -1) {
            dayColMap[hent.col] = d + 1;
            if (headerRow < 0) headerRow = hent.row;
          }
        }
      }
      if (Object.keys(dayColMap).length === 0) continue;

      // 识别每行对应的实际开始节次（从行首节次列读取数字，比纯行号更准）
      var rowStartSection = {};
      for (var m = 0; m < matrix.entries.length; m++) {
        var ment = matrix.entries[m];
        if (ment.row <= headerRow || dayColMap[ment.col]) continue;
        var mtxt = (ment.cell.textContent || '').trim();
        var secNum = mtxt.match(/^第?\s*(\d{1,2})\s*节?$/);
        if (secNum) {
          var parsedSec = parseInt(secNum[1], 10);
          if (parsedSec >= 1 && parsedSec <= 14) {
            rowStartSection[ment.row] = parsedSec;
          }
        }
      }

      for (var i = 0; i < matrix.entries.length; i++) {
        var ent = matrix.entries[i];
        if (ent.row <= headerRow) continue;
        var day = dayColMap[ent.col];
        if (!day) continue;

        var rawHtml = ent.cell.innerHTML || '';
        var blocks = rawHtml.split(/-----+|<hr[^>]*>|<br\s*\/?>\s*<br\s*\/?>/i);
        for (var b = 0; b < blocks.length; b++) {
          var lines = htmlToLines(doc, blocks[b]);
          if (lines.length === 0) continue;
          var name = lines[0];
          if (/^(第?\d+节|上午|下午|晚上|午休)$/.test(name)) continue;

          var weekOnlyStr = '';
          var location = '';
          var teacher = '';
          for (var k = 1; k < lines.length; k++) {
            var line = lines[k];
            if (line.indexOf('周') !== -1 || line.indexOf('节') !== -1) {
              weekOnlyStr += ' ' + line;
            } else if (!location && isLikelyRoom(line)) {
              location = cleanRoomText(line);
            } else if (!teacher) {
              teacher = cleanTeacherText(line);
            }
          }

          var startSec = rowStartSection[ent.row] || Math.max(1, ent.row - headerRow);
          var sec = parseSections(weekOnlyStr, startSec, ent.rowSpan);

          courses.push({
            name: name.replace(/【.*?】/g, '').trim(),
            teacher: teacher || '未知教师',
            location: location || '待定教室',
            dayOfWeek: day,
            startSection: sec[0],
            endSection: sec[1],
            weeks: parseWeeks(weekOnlyStr)
          });
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
